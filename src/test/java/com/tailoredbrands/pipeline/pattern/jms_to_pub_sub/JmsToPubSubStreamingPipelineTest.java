package com.tailoredbrands.pipeline.pattern.jms_to_pub_sub;

import com.tailoredbrands.util.coder.Coders;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.predef.Resources;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesCoder;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;

import static com.tailoredbrands.testutil.Matchers.hasItems;
import static com.tailoredbrands.testutil.Matchers.map;
import static com.tailoredbrands.testutil.TestUtils.jmsRecord;
import static com.tailoredbrands.testutil.TestUtils.pubsubMessage;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

public class JmsToPubSubStreamingPipelineTest implements Serializable {
    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void extractJMSPayload() {
        PCollection<Tuple2<JmsRecord, Try<String>>> pc = pipeline
                .apply(Create.of(jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml"))))
                .apply(JmsToPubSubStreamingPipeline.extractJmsPayload());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(t -> t._2.get(), containsString(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")))
                )
        );
        pipeline.run();
    }

    @Test
    public void toPubsubMessage() {
        PCollection<Tuple2<JmsRecord, Try<PubsubMessage>>> pc = pipeline
                .apply(Create.of(new Tuple2<>(
                        jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")),
                        Try.of(() -> JsonUtils.deserialize(Resources.readAsString("item_delta_feed/10_MAO_Item_JSON.json")))
                )).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(Coders.jsonNode()))))
                .apply(JmsToPubSubStreamingPipeline.toPubSubMessage());
        PAssert.that(pc).satisfies(it -> hasItems(it,
                map(message -> JsonUtils.deserialize(new String(message._2.get().getPayload())), CoreMatchers.is(JsonUtils.deserialize(Resources.readAsString("item_delta_feed/10_MAO_Item_JSON.json"))))
                )
        );
        pipeline.run();
    }

    @Test
    public void splitWhenSuccess() {
        TupleTag<PubsubMessage> successTag = new TupleTag<PubsubMessage>() {};
        TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag = new TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>>() {};
        PCollectionTuple tuple = pipeline
                .apply(Create.ofProvider(
                        ValueProvider.StaticValueProvider.of(
                                Tuple.of(
                                        jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")),
                                        Try.success(pubsubMessage(Resources.readAsString("item_delta_feed/10_MAO_Item_JSON.json")))
                                )
                        ),
                        Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(PubsubMessageWithAttributesCoder.of()))))
                .apply(JmsToPubSubStreamingPipeline.split(successTag, failureTag));
        // success
        PCollection<PubsubMessage> success = tuple.get(successTag).setCoder(PubsubMessageWithAttributesCoder.of());
        PAssert.that(success).satisfies(it -> hasItems(it,
                map(message -> JsonUtils.deserialize(new String(message.getPayload())), CoreMatchers.is(JsonUtils.deserialize(Resources.readAsString("item_delta_feed/10_MAO_Item_JSON.json"))))
                )
        );
        // failure
        PCollection<Tuple2<JmsRecord, Try<PubsubMessage>>> failure = tuple.get(failureTag)
                .setCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(PubsubMessageWithAttributesCoder.of())));
        PAssert.that(failure).empty();
        pipeline.run();
    }

    @Test
    public void splitWhenFailure() {
        TupleTag<PubsubMessage> successTag = new TupleTag<PubsubMessage>() {};
        TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>> failureTag = new TupleTag<Tuple2<JmsRecord, Try<PubsubMessage>>>() {};
        PCollectionTuple tuple = pipeline
                .apply(Create.ofProvider(
                        ValueProvider.StaticValueProvider.of(
                                Tuple.of(
                                        jmsRecord(Resources.readAsString("item_delta_feed/10_Universe_Item_xml.xml")),
                                        Try.failure(new RuntimeException("error"))
                                )
                        ),
                        Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(PubsubMessageWithAttributesCoder.of()))))
                .apply(JmsToPubSubStreamingPipeline.split(successTag, failureTag));
        // success
        PCollection<PubsubMessage> success = tuple.get(successTag).setCoder(PubsubMessageWithAttributesCoder.of());
        PAssert.that(success).empty();
        // failure
        PCollection<Tuple2<JmsRecord, Try<PubsubMessage>>> failure = tuple.get(failureTag)
                .setCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(PubsubMessageWithAttributesCoder.of())));
        PAssert.that(failure).satisfies(it -> hasItems(it,
                map(failureTuple -> failureTuple._2.failed().get(), is(instanceOf(RuntimeException.class)))));
        pipeline.run();
    }
}
