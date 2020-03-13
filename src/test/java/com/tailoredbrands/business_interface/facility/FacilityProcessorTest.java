package com.tailoredbrands.business_interface.facility;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ErrorType;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.generated.json.facitily.MAOLocation;
import com.tailoredbrands.generated.xml.facility.GetStoreResponseType;
import com.tailoredbrands.util.predef.Resources;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;

import static com.tailoredbrands.testutil.Matchers.hasItems;
import static com.tailoredbrands.testutil.Matchers.map;
import static com.tailoredbrands.testutil.TestUtils.jmsRecord;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

public class FacilityProcessorTest implements Serializable {
    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    public final String sourceFilePath = "facility/9_Universe_Facility_xml.xml";

    @Test
    public void processLocationTest() {
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString(sourceFilePath)), Try.success(Resources.readAsString(sourceFilePath)))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply("Xml to object", FacilityProcessor.convertXMLtoObject())
                .apply("Transform", FacilityProcessor.transformLocation())
                .apply("Object to JSON", FacilityProcessor.convertObjectToJson());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().get("Address").get("City").asText(), containsString("LAKE SAINT LOUIS")
                )
        ));
        pipeline.run();
    }

    @Test
    public void processInventoryLocationTest() {
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString(sourceFilePath)), Try.success(Resources.readAsString(sourceFilePath)))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply("Xml to object", FacilityProcessor.convertXMLtoObject())
                .apply("Transform", FacilityProcessor.transformInventoryLocation())
                .apply("Object to JSON", FacilityProcessor.convertObjectToJson());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().get("LocationStatusId").asText(), containsString("0")
                )
        ));
        pipeline.run();
    }

    @Test
    public void processLocationAttrTest() {
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString(sourceFilePath)), Try.success(Resources.readAsString(sourceFilePath)))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply("Xml to object", FacilityProcessor.convertXMLtoObject())
                .apply("Transform", FacilityProcessor.transformLocationAttributes())
                .apply("Object to JSON", FacilityProcessor.convertObjectToJson());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().get("LocationId").asText(), containsString("0775")
                )
        ));
        pipeline.run();
    }

    @Test
    public void convertXmlToItemObjectFailure() {
        PCollection<Tuple2<JmsRecord, Try<GetStoreResponseType>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString(sourceFilePath)), Try.success("DEFINITELY_NOT_AN_XML"))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply(FacilityProcessor.convertXMLtoObject());

        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> ((ProcessingException) t._2.failed().get()).getType(), CoreMatchers.is(ErrorType.XML_TO_OBJECT_CONVERSION_ERROR))
        ));
        pipeline.run();
    }

    @Test
    public void convertItemObjectToJson() {
        MAOLocation jsonItem = new MAOLocation();
        jsonItem.setLocationId("TEST");
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString(sourceFilePath)), Try.success(jsonItem))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(SerializableCoder.of(MAOLocation.class)))))
                .apply(FacilityProcessor.convertObjectToJson());

        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().get("LocationId").asText(), containsString("TEST")
                )
        ));
        pipeline.run();
    }
}
