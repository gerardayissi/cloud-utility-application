package com.tailoredbrands.business_interface.item_full_feed;

import com.tailoredbrands.generated.json.item_full_feed.CodeTypeId;
import com.tailoredbrands.generated.json.item_full_feed.Extended;
import com.tailoredbrands.generated.json.item_full_feed.HandlingAttributes;
import com.tailoredbrands.generated.json.item_full_feed.ItemCode;
import com.tailoredbrands.generated.json.item_full_feed.ItemFullFeed;
import com.tailoredbrands.generated.json.item_full_feed.ManufacturingAttribute;
import com.tailoredbrands.generated.json.item_full_feed.SellingAttributes;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.predef.Resources;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.PipelineRunner;
import org.apache.beam.sdk.coders.MapCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.tailoredbrands.testutil.Matchers.MapContainsAll.mapContainsAll;
import static com.tailoredbrands.testutil.Matchers.map;
import static com.tailoredbrands.testutil.Matchers.matchAll;

public class ItemFullFeedProcessorTest implements Serializable {
    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    private final String user = "admin@tbi.com";
    private final String organization = "TMW";

    @Test
    public void csvRowToDto() {
        val processor = new ItemFullFeedProcessor(getOptions());
        val pCollection = pipeline
                .apply(Create.ofProvider(
                        ValueProvider.StaticValueProvider.of(getItemFullFeedRow()),
                        MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())))
                .apply(processor.csvRowToDto());

        PAssert.that(pCollection).satisfies(dtos -> matchAll(dtos,
                map(tuple -> JsonUtils.toMap(tuple._2.get()), mapContainsAll(JsonUtils.toMap(getExpectedItem())))));
        pipeline.run();
    }

    @Test
    public void dtoToJson() {
        val processor = new ItemFullFeedProcessor(getOptions());
        val pCollection = pipeline
                .apply(Create.of(new Tuple2<>(getItemFullFeedRow(), Try.of(this::getExpectedItem)))
                        .withCoder(Tuple2Coder.of(MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()),
                                TryCoder.of(SerializableCoder.of(ItemFullFeed.class)))))
                .apply(processor.dtoToJson());

        PAssert.that(pCollection).satisfies(jsons -> matchAll(jsons,
                map(tuple -> JsonUtils.toMap(tuple._2.get()),
                        mapContainsAll(JsonUtils.toMap(JsonUtils.deserialize(Resources.readAsString("item_full_feed/item_full_feed_target.json")))))));
        pipeline.run();
    }

    @Test
    public void endToEnd() {
        val processor = new ItemFullFeedProcessor(getOptions());
        val pCollection = pipeline
                .apply(Create.ofProvider(
                        ValueProvider.StaticValueProvider.of(getItemFullFeedRow()),
                        MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())))
                .apply("CSV row to DTO", processor.csvRowToDto())
                .apply("DTO to JSON", processor.dtoToJson());

        PAssert.that(pCollection).satisfies(jsons -> matchAll(jsons,
                map(tuple -> JsonUtils.toMap(tuple._2.get()),
                        mapContainsAll(JsonUtils.toMap(JsonUtils.deserialize(Resources.readAsString("item_full_feed/item_full_feed_target.json")))))));
        pipeline.run();
    }

    public static Map<String, String> getItemFullFeedRow() {
        val row = new HashMap<String, String>();
        row.put("BASE.STORAGE.UOM", "U");
        row.put("COMPANY", "TMW");
        row.put("ITEMCODES.ID", "5K4960702");
        row.put("DIVISION.DESCRIPTION", "DRESS SHIRTS");
        row.put("DIVISION", "50");
        row.put("LONG.DESC", "CALVIN KLEIN BIG & TALL SPREAD SOLID SLIM FIT DRESS SHIRT");
        row.put("SEASON.CODE", "S16");
        row.put("WEIGHT.IN.LBS", "1");
        row.put("HAZARDOUS.FLAG", "");
        row.put("CLASS.DESC", "CK BTSOLSPRD");
        row.put("COUNTRY.OF.ORIGIN", "ID");
        row.put("VENDOR.NUM", "9524");
        row.put("COLOR.DESC", "GLACIER");
        row.put("FLAT.OR.GOH", "FLAT");
        row.put("GROUP", "51BSOLSPRDSLIM");
        row.put("PSEUDIV", "50");
        row.put("SIZE.DESC.MEDIUM", "16 36/37");
        row.put("VM.FOURGEN.ID", "1055859");
        row.put("CDF.SUPERGROUP", "5A2SOLIDSPRDCLR");
        row.put("CDF.SUPERGROUP.DESCRIPTION", "SOLIDSPRDCLR");
        row.put("SUBDIVISION", "5A1LS.SOLIDS");
        row.put("SUBDIVISION.DESCRIPTION", "LS.SOLIDS");
        row.put("SUPERDIV", "3");
        row.put("SUPERDIVISION.DESCRIPTION", "MEN");
        row.put("VENDOR.NAME", "PHILLIPS VAN HEUSEN CORP");
        row.put("CUSTOM.CLOTHING.FLAG", "");
        row.put("SIZE.DESC.SHORT", "16-67");
        return row;
    }

    private ItemFullFeed getExpectedItem() {
        val itemFullFeed = new ItemFullFeed();
        itemFullFeed.setBaseUOM("U");
        itemFullFeed.setBrand("TMW");
        itemFullFeed.setColor("GLACIER");
        itemFullFeed.setDepartmentName("DRESS SHIRTS");
        itemFullFeed.setDepartmentNumber("50");
        itemFullFeed.setDescription("CALVIN KLEIN BIG & TALL SPREAD SOLID SLIM FIT DRESS SHIRT");
        itemFullFeed.setIsGiftCard("false");
        itemFullFeed.setIsScanOnly("true");
        itemFullFeed.setIsGiftwithPurchase("false");
        itemFullFeed.setItemId("TMW5K4960702");
        itemFullFeed.setProductClass("5K49");
        itemFullFeed.setSeason("S16");
        itemFullFeed.setSeasonYear("2019");
        itemFullFeed.setShortDescription("CK BTSOLSPRD");
        itemFullFeed.setSize("16 36/37");
        itemFullFeed.setStyle("TMW");
        itemFullFeed.setWeight("1");
        itemFullFeed.setWeightUOM("LB");

        val handlingAttributes = new HandlingAttributes();
        handlingAttributes.setIsHazmat("false");
        handlingAttributes.setIsParcelShippingAllowed("true");
        handlingAttributes.setIsAirShippingAllowed("true");
        handlingAttributes.setDescription("CK BTSOLSPRD");
        itemFullFeed.setHandlingAttributes(handlingAttributes);

        val itemCode = new ItemCode();
        itemCode.setType("Primary UPC");
        val codeTypeId = new CodeTypeId();
        codeTypeId.setCodeTypeId("Primary UPC");
        itemCode.setCodeTypeId(codeTypeId);
        itemCode.setValue("5K4960702");
        itemFullFeed.getItemCode().add(itemCode);

        val manufacturingAttribute = new ManufacturingAttribute();
        manufacturingAttribute.setCountryofOrigin("ID");
        manufacturingAttribute.setVendorStyleNumber("9524");
        itemFullFeed.setManufacturingAttribute(manufacturingAttribute);

        val sellingAttributes = new SellingAttributes();
        sellingAttributes.setActivationRequired("false");
        sellingAttributes.setDigitalGoods("false");
        sellingAttributes.setIsDiscountable("false");
        sellingAttributes.setIsExchangeable("false");
        sellingAttributes.setIsPriceOverrideable("true");
        sellingAttributes.setIsReturnableAtDC("true");
        sellingAttributes.setShipToAddress("true");
        sellingAttributes.setPickUpInStore("true");
        sellingAttributes.setPriceStatusId("true");
        sellingAttributes.setSoldOnline("true");
        itemFullFeed.setSellingAttributes(sellingAttributes);

        val extended = new Extended();
        extended.setFlatOrGOH("FLAT");
        extended.setGroup("51BSOLSPRDSLIM");
        extended.setDivision("50");
        extended.setSizeDescriptionMedium("16 36/37");
        extended.setFourgenId("1055859");
        extended.setCDFSuperGroup("5A2SOLIDSPRDCLR");
        extended.setCDFSuperGroupDesc("SOLIDSPRDCLR");
        extended.setSubDivision("5A1LS.SOLIDS");
        extended.setSubDivisionDesc("LS.SOLIDS");
        extended.setSuperDivision("3");
        extended.setSuperDivisionDesc("MEN");
        extended.setVendorName("PHILLIPS VAN HEUSEN CORP");
        extended.setCustomClothingFlag("");
        extended.setEcomDescription("null");
        extended.setEcomProductName("null");
        extended.setEcomColor("null");
        extended.setEcomSize("null");
        itemFullFeed.setExtended(extended);

        return itemFullFeed;
    }

    private PipelineOptions getOptions() {
        return new GcsToPubSubOptions() {
            @Override
            public String getUser() {
                return null;
            }

            @Override
            public void setUser(String value) {

            }

            @Override
            public String getOrganization() {
                return null;
            }

            @Override
            public void setOrganization(String value) {

            }

            @Override
            public Long getDurationSeconds() {
                return null;
            }

            @Override
            public void setDurationSeconds(Long value) {

            }

            @Override
            public Integer getErrorThreshold() {
                return null;
            }

            @Override
            public void setErrorThreshold(Integer value) {

            }

            @Override
            public Integer getBatchPayloadSize() {
                return null;
            }

            @Override
            public void setBatchPayloadSize(Integer value) {

            }

            @Override
            public String getBusinessInterface() {
                return null;
            }

            @Override
            public void setBusinessInterface(String value) {

            }

            @Override
            public String getDelimiter() {
                return null;
            }

            @Override
            public void setDelimiter(String value) {

            }

            @Override
            public String getInputFilePattern() {
                return null;
            }

            @Override
            public void setInputFilePattern(String value) {

            }

            @Override
            public String getProcessedBucket() {
                return null;
            }

            @Override
            public void setProcessedBucket(String value) {

            }

            @Override
            public ValueProvider<String> getInputPubsubSubscription() {
                return null;
            }

            @Override
            public void setInputPubsubSubscription(ValueProvider<String> value) {

            }

            @Override
            public ValueProvider<String> getOutputPubsubTopic() {
                return null;
            }

            @Override
            public void setOutputPubsubTopic(ValueProvider<String> value) {

            }

            @Override
            public ValueProvider<String> getOutputTopics() {
                return null;
            }

            @Override
            public void setOutputTopics(ValueProvider<String> value) {

            }

            @Override
            public ValueProvider<String> getDeadletterPubsubTopic() {
                return null;
            }

            @Override
            public void setDeadletterPubsubTopic(ValueProvider<String> value) {

            }

            @Override
            public <T extends PipelineOptions> T as(Class<T> kls) {
                return null;
            }

            @Override
            public Class<? extends PipelineRunner<?>> getRunner() {
                return null;
            }

            @Override
            public void setRunner(Class<? extends PipelineRunner<?>> kls) {

            }

            @Override
            public CheckEnabled getStableUniqueNames() {
                return null;
            }

            @Override
            public void setStableUniqueNames(CheckEnabled enabled) {

            }

            @Override
            public String getTempLocation() {
                return null;
            }

            @Override
            public void setTempLocation(String value) {

            }

            @Override
            public String getJobName() {
                return null;
            }

            @Override
            public void setJobName(String jobName) {

            }

            @Override
            public Map<String, Map<String, Object>> outputRuntimeOptions() {
                return null;
            }

            @Override
            public long getOptionsId() {
                return 0;
            }

            @Override
            public void setOptionsId(long id) {

            }

            @Override
            public String getUserAgent() {
                return null;
            }

            @Override
            public void setUserAgent(String userAgent) {

            }

            @Override
            public void populateDisplayData(DisplayData.Builder builder) {

            }
        };
    }
}
