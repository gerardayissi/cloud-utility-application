package com.tailoredbrands.business_interface.facility;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.xml.XmlParser;
import com.tailoredbrands.generated.json.facitily.Address;
import com.tailoredbrands.generated.json.facitily.LocationFulfillmentAttr;
import com.tailoredbrands.generated.json.facitily.LocationStatus;
import com.tailoredbrands.generated.json.facitily.LocationType;
import com.tailoredbrands.generated.json.facitily.MAOInventoryLocation;
import com.tailoredbrands.generated.json.facitily.MAOLocation;
import com.tailoredbrands.generated.json.facitily.MAOLocationAttributes;
import com.tailoredbrands.generated.xml.facility.COType;
import com.tailoredbrands.generated.xml.facility.GetStoreResponseType;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_JSON_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.OBJECT_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.pipeline.error.ErrorType.XML_TO_OBJECT_CONVERSION_ERROR;
import static io.vavr.API.$;
import static io.vavr.API.Case;

public class FacilityProcessor extends PTransform<PCollection<Tuple2<JmsRecord, Try<String>>>, PCollectionList<Tuple2<JmsRecord, Try<JsonNode>>>> {
    @Override
    public PCollectionList<Tuple2<JmsRecord, Try<JsonNode>>> expand(PCollection<Tuple2<JmsRecord, Try<String>>> input) {
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> locationPC = input
                .apply("Convert XML string to POJO for MAOLocation", convertXMLtoObject())
                .apply("Transform POJO for MAOLocation", transformLocation())
                .apply("Convert POJO to MAOLocation JSON", convertObjectToJson());
        locationPC.setName("facility-location");

        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> locationInventoryPC = input
                .apply("Convert XML string to POJO for MAOInventoryLocation", convertXMLtoObject())
                .apply("Transform POJO for MAOInventoryLocation", transformInventoryLocation())
                .apply("Convert POJO to MAOInventoryLocation JSON", convertObjectToJson());
        locationInventoryPC.setName("facility-inventory-location");

        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> locationAttrPC = input
                .apply("Convert XML string to POJO for MAOLocationAttributes", convertXMLtoObject())
                .apply("Transform POJO for MAOLocationAttributes", transformLocationAttributes())
                .apply("Convert POJO to MAOLocationAttributes JSON", convertObjectToJson());
        locationAttrPC.setName("facility-location-attributes");

        PCollectionList<Tuple2<JmsRecord, Try<JsonNode>>> pcs = PCollectionList.of(locationPC).and(locationInventoryPC).and(locationAttrPC);

        return pcs;
    }

    static MapElements<Tuple2<JmsRecord, Try<String>>, Tuple2<JmsRecord, Try<GetStoreResponseType>>> convertXMLtoObject() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<GetStoreResponseType>>>() {
                })
                .via(tuple -> tuple.map2(
                        maybeXml -> maybeXml
                                .map(xml -> XmlParser.deserialize(xml, GetStoreResponseType.class))
                                .mapFailure(
                                        Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(XML_TO_OBJECT_CONVERSION_ERROR))
                                )
                ));
    }

    static MapElements<Tuple2<JmsRecord, Try<GetStoreResponseType>>, Tuple2<JmsRecord, Try<MAOLocation>>> transformLocation() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<MAOLocation>>>() {
                })
                .via(tuple -> tuple.map2(
                        root ->
                                root.map(xml -> {
                                    COType source = xml.getCO();
                                    MAOLocation json = new MAOLocation();

                                    json.setLocationId(source.getID());
                                    json.setOrganizationId(source.getCompanyCode());
                                    json.setDisplayId(source.getID());
                                    json.setLocationName(source.getName());
                                    json.setParentOrgId(source.getCompanyCode());
                                    json.setLocationTimeZone("America/Chicago");

                                    Address address = new Address();
                                    address.setAddress1(source.getAddr());
                                    address.setCity(source.getCity());
                                    address.setState(source.getSt());
                                    address.setPostalCode(source.getZip().substring(0, 4));
                                    address.setCountry(source.getCountry());
                                    address.setCounty(source.getCounty());
                                    address.setPhone(source.getPhone());
                                    address.setEmail(source.getEmailAddress());
                                    json.setAddress(address);

                                    LocationType locationType = new LocationType();
                                    locationType.setLocationTypeId((source.getStoreType() == "S") ? "Store" : (source.getStoreType() == "W") ? "DC" : "null");
                                    json.setLocationType(locationType);

                                    LocationFulfillmentAttr locationFulfillmentAttr = new LocationFulfillmentAttr();
                                    locationFulfillmentAttr.setPickupAtStore(source.getPickupInStore());
                                    locationFulfillmentAttr.setShipFromStore(source.getShipFromFacility());
                                    locationFulfillmentAttr.setShipToStore(source.getShipToStore());
                                    json.setLocationFulfillmentAttr(locationFulfillmentAttr);

                                    LocationStatus locationStatus = new LocationStatus();
                                    locationStatus.setLocationStatusId("Operational");
                                    json.setLocationStatus(locationStatus);

                                    return json;
                                })
                                        .mapFailure(
                                                Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_OBJECT_CONVERSION_ERROR, exc))
                                        )
                ));
    }

    static MapElements<Tuple2<JmsRecord, Try<GetStoreResponseType>>, Tuple2<JmsRecord, Try<MAOInventoryLocation>>> transformInventoryLocation() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<MAOInventoryLocation>>>() {
                })
                .via(tuple -> tuple.map2(
                        root ->
                                root.map(xml -> {
                                    COType source = xml.getCO();
                                    MAOInventoryLocation json = new MAOInventoryLocation();
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);

                                    json.setLocationId(source.getID());
                                    json.setDisplayId(source.getID());
                                    json.setLocationStatusId(
                                            (source.getDateClose() == null || source.getDateClose().trim().isEmpty()) ? "0" : ChronoUnit.DAYS.between(LocalDate.parse(source.getDateClose(), formatter), LocalDate.now()) >= 0 ? "1" : "0"
                                    );
                                    json.setLocationTypeId("Stores");
                                    json.setLocationName(source.getID());
                                    json.setLaborCost("1.0");
                                    json.setCapacityUtilizationLevel("Order");
                                    json.setPickupAtStore(source.getPickupInStore());
                                    json.setShipFromStore(source.getShipFromFacility());
                                    json.setShipToStore(source.getShipToStore());

                                    return json;
                                })
                                        .mapFailure(
                                                Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_OBJECT_CONVERSION_ERROR, exc))
                                        )
                ));
    }

    static MapElements<Tuple2<JmsRecord, Try<GetStoreResponseType>>, Tuple2<JmsRecord, Try<MAOLocationAttributes>>> transformLocationAttributes() {
        return MapElements
                .into(new TypeDescriptor<Tuple2<JmsRecord, Try<MAOLocationAttributes>>>() {
                })
                .via(tuple -> tuple.map2(
                        root ->
                                root.map(xml -> {
                                    COType source = xml.getCO();
                                    MAOLocationAttributes json = new MAOLocationAttributes();

                                    json.setLocationId(source.getID());
                                    json.setMaintainOHSupply("true");
                                    json.setMaintainOHByCountryOfOrigin("true");
                                    json.setMaintainOHByInventoryType("true");

                                    return json;
                                })
                                        .mapFailure(
                                                Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_OBJECT_CONVERSION_ERROR, exc))
                                        )
                ));
    }


    static <T> MapElements<Tuple2<JmsRecord, Try<T>>, Tuple2<JmsRecord, Try<JsonNode>>> convertObjectToJson() {
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
