package com.tailoredbrands.business_interface.item_full_feed;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.tailoredbrands.generated.json.item_full_feed.CodeTypeId;
import com.tailoredbrands.generated.json.item_full_feed.Extended;
import com.tailoredbrands.generated.json.item_full_feed.HandlingAttributes;
import com.tailoredbrands.generated.json.item_full_feed.ItemCode;
import com.tailoredbrands.generated.json.item_full_feed.ItemFullFeed;
import com.tailoredbrands.generated.json.item_full_feed.ManufacturingAttribute;
import com.tailoredbrands.generated.json.item_full_feed.SellingAttributes;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub.GcsToPubSubCounter;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.extensions.gcp.options.GcsOptions;
import org.apache.beam.sdk.extensions.gcp.util.GcsUtil;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import static com.google.api.gax.batching.FlowController.LimitExceededBehavior;
import static com.tailoredbrands.pipeline.error.ErrorType.CSV_ROW_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static com.tailoredbrands.util.FileUtils.getProcessedFilePath;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.beam.repackaged.core.org.apache.commons.lang3.StringUtils.truncate;
import static org.apache.beam.repackaged.core.org.apache.commons.lang3.math.NumberUtils.toDouble;

public class ItemFullFeedProcessFileFn extends DoFn<FileIO.ReadableFile, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(ItemFullFeedProcessFileFn.class);

    private static final Base64.Encoder Base64Encoder = Base64.getEncoder();

    private final GcsToPubSubCounter counter;
    private final String user;
    private final String organization;
    private final String outputPubsubTopic;
    private final char delimiter;

    private Publisher publisher;

    public ItemFullFeedProcessFileFn(GcsToPubSubOptions options, GcsToPubSubCounter counter) {
        this.counter = counter;
        user = options.getUser();
        organization = options.getOrganization();
        outputPubsubTopic = options.getOutputPubsubTopic().get();
        val delimiterStr = options.getDelimiter();
        if (delimiterStr.length() > 1) {
            throw new IllegalArgumentException("Delimiter length exceedes 1 char: " + delimiterStr);
        }
        delimiter = delimiterStr.charAt(0);
    }

    @Setup
    public void setup() {
        try {
            publisher = Publisher.newBuilder(outputPubsubTopic)
                    .setBatchingSettings(
                            BatchingSettings.newBuilder()
                                    .setElementCountThreshold(1000L)
                                    .setRequestByteThreshold(1024 * 1024L)
                                    .setDelayThreshold(Duration.ofSeconds(1))
                                    .setFlowControlSettings(
                                            FlowControlSettings.newBuilder()
                                                    .setLimitExceededBehavior(LimitExceededBehavior.Ignore)
                                                    .build())
                                    .build())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Teardown
    public void teardown() {
        try {
            if (publisher != null) {
                publisher.shutdown();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ProcessElement
    public void process(ProcessContext processContext) throws Exception {
        val gcsUtil = processContext.getPipelineOptions().as(GcsOptions.class).getGcsUtil();
        val filePath = processContext.element().getMetadata().resourceId().toString();
        LOG.info("File: {}, Start processing", filePath);
        val is = Channels.newInputStream(processContext.element().open());
        val reader = new InputStreamReader(is);
        val records = parseToRecords(reader);
        val pubSubFutures = new ArrayList<ApiFuture<String>>();
        for (CSVRecord record : records) {
            counter.csvRowsRead.inc();
            val maybeData = processRecord(record);
            if (maybeData.isSuccess()) {
                pubSubFutures.add(publisher.publish(toPubSubMessage(maybeData)));
                counter.pubsubMessagesWritten.inc();
            } else {
                processError(record, maybeData);
            }
        }
        publisher.publishAllOutstanding();
        ApiFutures.allAsList(pubSubFutures).get();
        reader.close();
        is.close();
        LOG.info("File: {}, {} messages published", filePath, pubSubFutures.size());
        moveToProcessed(gcsUtil, filePath);
    }

    private CSVParser parseToRecords(InputStreamReader reader) throws IOException {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withDelimiter(delimiter)
                .withTrim(true)
                .parse(reader);
    }

    @SuppressWarnings("unchecked")
    private Try<String> processRecord(CSVRecord record) {
        val maybeItem = Try.of(() -> toItemFullFeed(record.toMap()))
                .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(CSV_ROW_TO_OBJECT_CONVERSION_ERROR, exc)));
        return maybeItem
                .map(JsonUtils::toJsonNode)
                .map(JsonUtils::serializeToBytes)
                .map(Base64Encoder::encodeToString)
                .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc)));
    }

    private ItemFullFeed toItemFullFeed(Map<String, String> csvRow) {
        val itemFullFeed = new ItemFullFeed();
        itemFullFeed.setBaseUOM("U");
        val company = csvRow.get("COMPANY");
        itemFullFeed.setBrand(!company.isEmpty() ? company : truncate(csvRow.get("ITEMCODES.ID"), 3));
        itemFullFeed.setColor(csvRow.get("COLOR.DESC"));
        itemFullFeed.setDepartmentName(csvRow.get("DIVISION.DESCRIPTION"));
        itemFullFeed.setDepartmentNumber(csvRow.get("DIVISION"));
        itemFullFeed.setDescription(csvRow.get("LONG.DESC"));
        itemFullFeed.setIsGiftCard(false);
        itemFullFeed.setIsScanOnly(true);
        itemFullFeed.setIsGiftwithPurchase(false);
        itemFullFeed.setItemId(csvRow.get("COMPANY") + csvRow.get("ITEMCODES.ID"));
        itemFullFeed.setProductClass(getProductClass(csvRow));
        itemFullFeed.setSeason(csvRow.get("SEASON.CODE"));
        itemFullFeed.setSeasonYear(2019); //todo: verify with stakeholders
        itemFullFeed.setShortDescription(truncate(csvRow.get("CLASS.DESC"), 50));
        itemFullFeed.setSize(csvRow.get("SIZE.DESC.MEDIUM"));
        itemFullFeed.setStyle("FLAT".equalsIgnoreCase(csvRow.get("FLAT.OR.GOH")) ? "FLT" : "GOH");
        itemFullFeed.setWeight(toDouble(csvRow.get("WEIGHT.IN.LBS")));
        itemFullFeed.setWeightUOM("LB");

        val handlingAttributes = new HandlingAttributes();
        handlingAttributes.setIsAirShippingAllowed(!"true".equalsIgnoreCase(csvRow.get("HAZARDOUS.FLAG")));
        handlingAttributes.setIsHazmat("1".equals(csvRow.get("HAZARDOUS.FLAG")));
        handlingAttributes.setIsParcelShippingAllowed(true);
        handlingAttributes.setDescription(csvRow.get("CLASS.DESC"));
        itemFullFeed.setHandlingAttributes(handlingAttributes);

        val itemCode = new ItemCode();
        itemCode.setType("Primary UPC");
        val codeTypeId = new CodeTypeId();
        codeTypeId.setCodeTypeId("Primary UPC");
        itemCode.setCodeTypeId(codeTypeId);
        itemCode.setValue(csvRow.get("ITEMCODES.ID"));
        itemFullFeed.getItemCode().add(itemCode);

        val manufacturingAttribute = new ManufacturingAttribute();
        manufacturingAttribute.setCountryofOrigin(csvRow.get("COUNTRY.OF.ORIGIN"));
        manufacturingAttribute.setVendorStyleNumber(csvRow.get("VENDOR.NUM"));
        itemFullFeed.setManufacturingAttribute(manufacturingAttribute);

        val sellingAttributes = new SellingAttributes();
        sellingAttributes.setActivationRequired(false);
        sellingAttributes.setDigitalGoods(false);
        sellingAttributes.setIsDiscountable(false);
        sellingAttributes.setIsExchangeable(false);
        sellingAttributes.setIsPriceOverrideable(true);
        sellingAttributes.setIsReturnableAtDC(true);
        sellingAttributes.setShipToAddress(true);
        sellingAttributes.setPickUpInStore(true);
        sellingAttributes.setPriceStatusId("true");
        sellingAttributes.setSoldOnline(true);
        itemFullFeed.setSellingAttributes(sellingAttributes);

        val extended = new Extended();
        extended.setFlatOrGOH("FLAT".equalsIgnoreCase(csvRow.get("FLAT.OR.GOH")) ? "FLT" : "GOH");
        extended.setGroup(csvRow.get("GROUP"));
        extended.setDivision(csvRow.get("PSEUDIV"));
        extended.setSizeDescriptionMedium(csvRow.get("SIZE.DESC.MEDIUM"));
        extended.setFourgenId(csvRow.get("VM.FOURGEN.ID"));
        extended.setCDFSuperGroup(csvRow.get("CDF.SUPERGROUP"));
        extended.setCDFSuperGroupDesc(csvRow.get("CDF.SUPERGROUP.DESCRIPTION"));
        extended.setSubDivision(csvRow.get("SUBDIVISION"));
        extended.setSubDivisionDesc(csvRow.get("SUBDIVISION.DESCRIPTION"));
        extended.setSuperDivision(csvRow.get("SUPERDIV"));
        extended.setSuperDivisionDesc(csvRow.get("SUPERDIVISION.DESCRIPTION"));
        extended.setVendorName(csvRow.get("VENDOR.NAME"));
        extended.setCustomClothingFlag(csvRow.get("CUSTOM.CLOTHING.FLAG"));
        extended.setEcomDescription("null");
        extended.setEcomProductName("null");
        extended.setEcomColor("null");
        extended.setEcomSize("null");
        itemFullFeed.setExtended(extended);

        return itemFullFeed;
    }

    private String getProductClass(Map<String, String> csvRow) {
        if ("1".equals(csvRow.get("NO.WAREHOUSE.STOCK"))) return "DROPSHIP";
        if ("838CEDARSHOETRE".equalsIgnoreCase(csvRow.get("GROUP"))) return "SHOETREE";
        if ("853UMBRELLAS".equalsIgnoreCase(csvRow.get("GROUP"))) return "UMBRELLA";
        if ("40".equalsIgnoreCase(csvRow.get("PSEUDIV"))) return "SHOE";
        return truncate(csvRow.get("ITEMCODES.ID"), 4);
    }

    private PubsubMessage toPubSubMessage(Try<String> maybeData) {
        return PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(maybeData.get()))
                .putAttributes("User", user)
                .putAttributes("Organization", organization)
                .build();
    }

    private void processError(CSVRecord record, Try<String> maybeData) {
        val err = (ProcessingException) maybeData.failed().get();
        val detailedCounter = Match(err.getType()).of(
                Case($(CSV_ROW_TO_OBJECT_CONVERSION_ERROR), counter.csvRowToObjectErrors),
                Case($(OBJECT_TO_JSON_CONVERSION_ERROR), counter.objectToJsonErrors),
                Case($(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR), counter.jsonToPubSubErrors),
                Case($(), counter.untypedErrors)
        );
        detailedCounter.inc();
        counter.totalErrors.inc();
        LOG.error(format("Failed to process Row: %s", record.toMap()), err);
    }

    private void moveToProcessed(GcsUtil gcsUtil, String filePath) throws IOException {
        val processedFiledPath = getProcessedFilePath(filePath);
        gcsUtil.copy(asList(filePath), asList(processedFiledPath));
        gcsUtil.remove(asList(filePath));
        LOG.info("File: {}, Moved to: {}", filePath, processedFiledPath);
    }
}
