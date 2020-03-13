package com.tailoredbrands.business_interface.item_delta_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.generated.json.item_delta_feed.MAOItem;
import com.tailoredbrands.generated.xml.item_delta_feed.RootType;
import com.tailoredbrands.pipeline.error.ErrorType;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.util.predef.Resources;
import com.tailoredbrands.util.xml.XmlParser;
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

public class ItemDeltaFeedProcessorTest implements Serializable {
    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void convertXmlToItemObject() {
        PCollection<Tuple2<JmsRecord, Try<RootType>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")), Try.success(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply(ItemDeltaFeedProcessor.convertXMLtoObject());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().getItemcode().getID(), containsString("52R465567")
                )
        ));
        pipeline.run();
    }

    @Test
    public void convertXmlToItemObjectFailure() {
        PCollection<Tuple2<JmsRecord, Try<RootType>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")), Try.success("DEFINITELY_NOT_AN_XML"))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply(ItemDeltaFeedProcessor.convertXMLtoObject());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> ((ProcessingException) t._2.failed().get()).getType(), CoreMatchers.is(ErrorType.XML_TO_OBJECT_CONVERSION_ERROR))
        ));
        pipeline.run();
    }

    @Test
    public void convertItemObjectToJson() {
        MAOItem jsonItem = new MAOItem();
        jsonItem.setItemId("TEST");
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")), Try.success(jsonItem))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(SerializableCoder.of(MAOItem.class)))))
                .apply(ItemDeltaFeedProcessor.convertObjectToJson());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().get("ItemId").asText(), containsString("TEST")
                )
        ));
        pipeline.run();
    }

    @Test
    public void objectTransformation() {
        RootType xmlRoot = XmlParser.deserialize(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml"), RootType.class);
        PCollection<Tuple2<JmsRecord, Try<MAOItem>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")), Try.success(xmlRoot))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(SerializableCoder.of(RootType.class)))))
                .apply(ItemDeltaFeedProcessor.transform());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().getItemId(), containsString(xmlRoot.getItemcode().getCompany().concat(xmlRoot.getItemcode().getID()))
                )
        ));
        pipeline.run();
    }

    @Test
    public void endToEndProcessorTest() {
        PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
                .apply(Create.of(
                        new Tuple2<>(jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")), Try.success(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")))
                ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
                .apply("Xml to object", ItemDeltaFeedProcessor.convertXMLtoObject())
                .apply("Transform", ItemDeltaFeedProcessor.transform())
                .apply("Object to JSON", ItemDeltaFeedProcessor.convertObjectToJson());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get().get("ItemId").asText(), containsString("TMW52R465567")
                )
        ));
        pipeline.run();
    }
}
