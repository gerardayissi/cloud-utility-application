package com.tailoredbrands.business_interface.item_delta_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.xml.XmlParser;
import com.tailoredbrands.generated.json.item_delta_feed.CodeTypeId;
import com.tailoredbrands.generated.json.item_delta_feed.Extended;
import com.tailoredbrands.generated.json.item_delta_feed.HandlingAttributes;
import com.tailoredbrands.generated.json.item_delta_feed.ItemCode;
import com.tailoredbrands.generated.json.item_delta_feed.MAOItem;
import com.tailoredbrands.generated.json.item_delta_feed.ManufacturingAttribute;
import com.tailoredbrands.generated.json.item_delta_feed.SellingAttributes;
import com.tailoredbrands.generated.xml.item_delta_feed.ItemcodeType;
import com.tailoredbrands.generated.xml.item_delta_feed.RootType;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;

import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.XML_TO_OBJECT_CONVERSION_ERROR;
import static io.vavr.API.$;
import static io.vavr.API.Case;

public class ItemDeltaFeedProcessor extends PTransform<PCollection<Tuple2<JmsRecord, Try<String>>>, PCollection<Tuple2<JmsRecord, Try<JsonNode>>>> {
    @Override
    public PCollection<Tuple2<JmsRecord, Try<JsonNode>>> expand(PCollection<Tuple2<JmsRecord, Try<String>>> input) {
        return input
                .apply("Convert XML string to POJO", convertXMLtoObject())
                .apply("Transform POJO", transform())
                .apply("Convert POJO to JSON", convertObjectToJson());
    }

    @SuppressWarnings("unchecked")
    static MapElements<Tuple2<JmsRecord, Try<String>>, Tuple2<JmsRecord, Try<RootType>>> convertXMLtoObject() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<RootType>>>() {
                })
                .via(tuple -> tuple.map2(
                        maybeXml -> maybeXml
                                .map(xml -> XmlParser.deserialize(xml, RootType.class))
                                .mapFailure(
                                        Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(XML_TO_OBJECT_CONVERSION_ERROR, exc))
                                )
                ));
    }

    @SuppressWarnings("unchecked")
    static MapElements<Tuple2<JmsRecord, Try<RootType>>, Tuple2<JmsRecord, Try<MAOItem>>> transform() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<MAOItem>>>() {
                })
                .via(tuple -> tuple.map2(
                        root ->
                                root.map(xml -> {
                                    ItemcodeType itemcode = xml.getItemcode();
                                    String itemID = itemcode.getID();
                                    String company = itemcode.getCompany();
                                    String colorDesc = itemcode.getCOLORDESC();

                                    MAOItem json = new MAOItem();
                                    json.setBaseUOM("U");
                                    json.setBrand(company.isEmpty() ? itemID.substring(0, 3) : company);
                                    json.setColor(colorDesc.isEmpty() ? itemID.substring(itemID.length() - 2) : colorDesc);
                                    json.setDepartmentName(itemcode.getDIVISIONDESCRIPTION());
                                    json.setDepartmentNumber(itemcode.getDIV());
                                    json.setDescription(itemcode.getLONGDESC());
                                    json.setIsGiftCard(false);
                                    json.setItemId(company.concat(itemID));
                                    json.setProductClass(itemID.substring(0, 4));
                                    json.setSeason(itemcode.getSEASONCODE());
                                    json.setSeasonYear(2019);
                                    json.setShortDescription(itemcode.getLONGDESC());
                                    json.setSize(itemcode.getSIZEDESCMEDIUM());
                                    json.setStyle(company.isEmpty() ? itemID.substring(0, 3) : company);
                                    json.setWeight(Double.parseDouble(itemcode.getWEIGHTINLBS()));
                                    json.setWeightUOM("LB");

                                    HandlingAttributes hand = new HandlingAttributes();
                                    hand.setIsAirShippingAllowed(!Boolean.parseBoolean(itemcode.getHAZARDOUSFLAG()));
                                    hand.setIsHazmat(Boolean.parseBoolean(itemcode.getHAZARDOUSFLAG()));
                                    hand.setIsParcelShippingAllowed(true);
                                    hand.setDescription(itemcode.getCLASSDESC());
                                    json.setHandlingAttributes(hand);

                                    ItemCode jsonItemcode = new ItemCode();
                                    jsonItemcode.setType("Primary_UPC");
                                    jsonItemcode.setValue(itemID);
                                    jsonItemcode.setCodeTypeId(new CodeTypeId("Primary_UPC"));
                                    json.setItemCode(List.of(jsonItemcode).asJava());

                                    ManufacturingAttribute manAttr = new ManufacturingAttribute();
                                    manAttr.setCountryofOrigin(itemcode.getCOUNTRYOFORIGIN());
                                    manAttr.setVendorStyleNumber(itemcode.getVENDORNUM());
                                    json.setManufacturingAttribute(manAttr);

                                    SellingAttributes selAttr = new SellingAttributes();
                                    selAttr.setActivationRequired(false);
                                    selAttr.setDigitalGoods(false);
                                    selAttr.setIsPriceOverrideable(true);
                                    selAttr.setIsReturnableAtDC(true);
                                    selAttr.setPickUpInStore(true);
                                    selAttr.setShipToAddress(true);
                                    selAttr.setSoldOnline(true);
                                    json.setSellingAttributes(selAttr);

                                    Extended extended = new Extended();
                                    extended.setFlatOrGOH(itemcode.getFLATORGOH());
                                    extended.setGroup(itemcode.getGROUP());
                                    extended.setDivision(itemcode.getPSEUDIV());
                                    extended.setSizeDescriptionMedium(itemcode.getSIZEDESCMEDIUM());
                                    extended.setFourgenId(itemcode.getVMFOURGENID());
                                    extended.setCDFSuperGroup(itemcode.getCDFSUPERGROUP());
                                    extended.setCDFSuperGroupDesc(itemcode.getCDFSUPERGROUPDESCRIPTION());
                                    extended.setSubDivision(itemcode.getSUBDIVISION());
                                    extended.setSubDivisionDesc(itemcode.getSUBDIVISIONDESCRIPTION());
                                    extended.setSuperDivision(itemcode.getSUPERDIVISION());
                                    extended.setSuperDivisionDesc(itemcode.getSUPERDIVISIONDESCRIPTION());
                                    extended.setVendorName(itemcode.getVENDORNAME());
                                    json.setExtended(extended);

                                    return json;
                                })
                                        .mapFailure(
                                                Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_OBJECT_CONVERSION_ERROR, exc))
                                        )
                ));
    }

    @SuppressWarnings("unchecked")
    static MapElements<Tuple2<JmsRecord, Try<MAOItem>>, Tuple2<JmsRecord, Try<JsonNode>>> convertObjectToJson() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<JsonNode>>>() {
                })
                .via(tuple -> tuple.map2(
                        item ->
                                item
                                        .map(JsonUtils::toJsonNode)
                                        .mapFailure(
                                                Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))
                                        )
                ));
    }
}
