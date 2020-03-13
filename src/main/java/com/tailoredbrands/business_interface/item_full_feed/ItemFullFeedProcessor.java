package com.tailoredbrands.business_interface.item_full_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.generated.json.item_full_feed.CodeTypeId;
import com.tailoredbrands.generated.json.item_full_feed.Extended;
import com.tailoredbrands.generated.json.item_full_feed.HandlingAttributes;
import com.tailoredbrands.generated.json.item_full_feed.ItemCode;
import com.tailoredbrands.generated.json.item_full_feed.ItemFullFeed;
import com.tailoredbrands.generated.json.item_full_feed.ManufacturingAttribute;
import com.tailoredbrands.generated.json.item_full_feed.SellingAttributes;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tailoredbrands.pipeline.error.ErrorType.CSV_ROW_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static java.lang.Math.min;
import static java.util.Collections.singletonList;

public class ItemFullFeedProcessor extends PTransform<PCollection<Map<String, String>>,
        PCollection<Tuple2<Map<String, String>, Try<JsonNode>>>> {

    private static final Base64.Encoder Base64Encoder = Base64.getEncoder();

    private String user;
    private String organization;

    public ItemFullFeedProcessor(PipelineOptions options) {
        if (options instanceof GcsToPubSubOptions) {
            val gcsToPubSubOptions = (GcsToPubSubOptions) options;
            user = gcsToPubSubOptions.getUser();
            organization = gcsToPubSubOptions.getOrganization();
        } else {
            throw new IllegalArgumentException("Invalid Item Full Feed options: " + options.getClass().getSimpleName());
        }
    }

    @Override
    public PCollection<Tuple2<Map<String, String>, Try<JsonNode>>> expand(PCollection<Map<String, String>> rows) {
        return rows
                .apply("CSV row to DTO", csvRowToDto())
                .apply("DTO to JSON", dtoToJson());
    }

    @SuppressWarnings("unchecked")
    MapElements<Map<String, String>, Tuple2<Map<String, String>, Try<ItemFullFeed>>> csvRowToDto() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<Map<String, String>, Try<ItemFullFeed>>>() {
                })
                .via(csvRow -> new Tuple2<>(csvRow, Try.of(() -> toItemFullFeed(csvRow))
                        .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                                exc -> new ProcessingException(CSV_ROW_TO_OBJECT_CONVERSION_ERROR, exc))))
                );
    }

    private ItemFullFeed toItemFullFeed(Map<String, String> csvRow) {
        val itemFullFeed = new ItemFullFeed();
        itemFullFeed.setBaseUOM("U");
        itemFullFeed.setBrand(csvRow.get("COMPANY"));
        itemFullFeed.setColor(csvRow.get("COLOR.DESC"));
        itemFullFeed.setDepartmentName(csvRow.get("DIVISION.DESCRIPTION"));
        itemFullFeed.setDepartmentNumber(csvRow.get("DIVISION"));
        itemFullFeed.setDescription(csvRow.get("LONG.DESC"));
        itemFullFeed.setIsGiftCard("false");
        itemFullFeed.setIsScanOnly("true");
        itemFullFeed.setIsGiftwithPurchase("false");
        itemFullFeed.setItemId(csvRow.get("COMPANY") + csvRow.get("ITEMCODES.ID"));
        val id = csvRow.getOrDefault("ITEMCODES.ID", "").trim();
        val idTruncateAt = min(id.length(), 4);
        itemFullFeed.setProductClass(id.substring(0, idTruncateAt));
        itemFullFeed.setSeason(csvRow.get("SEASON.CODE"));
        itemFullFeed.setSeasonYear("2019"); //todo: verify with stakeholders
        val shortDesc = csvRow.getOrDefault("CLASS.DESC", "").trim();
        val shortDescTruncateAt = min(shortDesc.length(), 50);
        itemFullFeed.setShortDescription(shortDesc.substring(0, shortDescTruncateAt));
        itemFullFeed.setSize(csvRow.get("SIZE.DESC.MEDIUM"));
        itemFullFeed.setStyle(csvRow.get("COMPANY"));
        itemFullFeed.setWeight(csvRow.get("WEIGHT.IN.LBS"));
        itemFullFeed.setWeightUOM("LB");

        val handlingAttributes = new HandlingAttributes();
        handlingAttributes.setIsHazmat("1".equals(csvRow.get("HAZARDOUS.FLAG")) ? "true" : "false");
        handlingAttributes.setIsParcelShippingAllowed("true");
        handlingAttributes.setIsAirShippingAllowed("true".equalsIgnoreCase(csvRow.get("HAZARDOUS.FLAG")) ? "false" : "true");
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
        extended.setFlatOrGOH(csvRow.get("FLAT.OR.GOH"));
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

    @SuppressWarnings("unchecked")
    MapElements<Tuple2<Map<String, String>, Try<ItemFullFeed>>, Tuple2<Map<String, String>, Try<JsonNode>>> dtoToJson() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<Map<String, String>, Try<JsonNode>>>() {
                })
                .via(tuple -> tuple.map2(
                        maybeItem -> maybeItem
                                .map(JsonUtils::toJsonNode)
                                .map(JsonUtils::serializeToBytes)
                                .map(Base64Encoder::encodeToString)
                                .map(this::toJsonWithAttributes)
                                .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                                        exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))))
                );
    }

    private JsonNode toJsonWithAttributes(String base64string) {
        val attributes = new LinkedHashMap<String, String>(2);
        attributes.put("User", user);
        attributes.put("Organization", organization);

        val message = new LinkedHashMap<String, Object>(2);
        message.put("attributes", attributes);
        message.put("data", base64string);

        val payload = new HashMap<String, List<Map<String, Object>>>(1);
        payload.put("messages", singletonList(message));

        return JsonUtils.toJsonNode(payload);
    }
}
