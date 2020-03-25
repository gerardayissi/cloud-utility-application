package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.function.CsvFileToRowsFn;
import com.tailoredbrands.util.coder.Coders;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.coders.MapCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesCoder;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.TupleTag;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Map;

import static com.tailoredbrands.business_interface.item_full_feed.ItemFullFeedProcessorTest.getItemFullFeedRow;
import static com.tailoredbrands.pipeline.error.ErrorType.OTHER;
import static com.tailoredbrands.testutil.Matchers.MapContainsAll.mapContainsAll;
import static com.tailoredbrands.testutil.Matchers.map;
import static com.tailoredbrands.testutil.Matchers.matchAll;
import static com.tailoredbrands.testutil.TestUtils.getAbsolutePath;
import static com.tailoredbrands.testutil.TestUtils.pubsubMessage;
import static com.tailoredbrands.util.predef.Resources.readAsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

public class GcsToPubSubStreamingPipelineTest implements Serializable {
    @Rule
    public transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void csvFileToRows() {
        val expectedRow = getItemFullFeedRow();
        val pCollection = pipeline
                .apply(FileIO.match().filepattern(getAbsolutePath("item_full_feed/item_full_feed_source.csv")))
                .apply(FileIO.readMatches())
                .apply(ParDo.of(new CsvFileToRowsFn("|")));

        PAssert.that(pCollection).satisfies(rows -> matchAll(rows, mapContainsAll(expectedRow)));
        pipeline.run();
    }

    @Test
    public void toPubSubMessage() {
        val pCollection = pipeline
                .apply(Create.of(new Tuple2<>(getItemFullFeedRow(), Try.of(() ->
                        JsonUtils.deserialize(readAsString("item_full_feed/item_full_feed_target.json")))))
                        .withCoder(Tuple2Coder.of(MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()),
                                TryCoder.of(Coders.jsonNode()))))
                .apply(GcsToPubSubStreamingPipeline.toPubSubMessage());

        PAssert.that(pCollection)
                .satisfies(messages -> matchAll(messages,
                        map(message -> JsonUtils.deserialize(new String(message._2.get().getPayload())),
                                is(JsonUtils.deserialize(readAsString("item_full_feed/item_full_feed_target.json"))))));
        pipeline.run();
    }

    @Test
    public void splitWhenSuccess() {
        val successTag = new TupleTag<PubsubMessage>() {};
        val failureTag = new TupleTag<Tuple2<Map<String, String>, Try<PubsubMessage>>>() {};
        val pCollectionTuple = pipeline
                .apply(Create.ofProvider(
                        ValueProvider.StaticValueProvider.of(
                                Tuple.of(
                                        getItemFullFeedRow(),
                                        Try.success(pubsubMessage(readAsString("item_full_feed/item_full_feed_target.json")))
                                )
                        ), Tuple2Coder.of(MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()),
                                TryCoder.of(PubsubMessageWithAttributesCoder.of()))))
                .apply(GcsToPubSubStreamingPipeline.split(successTag, failureTag));
        // success
        val successCollection = pCollectionTuple.get(successTag).setCoder(PubsubMessageWithAttributesCoder.of());
        PAssert.that(successCollection)
                .satisfies(messages -> matchAll(messages,
                        map(message -> JsonUtils.deserialize(new String(message.getPayload())),
                                is(JsonUtils.deserialize(readAsString("item_full_feed/item_full_feed_target.json"))))));
        // failure
        val failureCollection = pCollectionTuple.get(failureTag)
                .setCoder(Tuple2Coder.of(MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()),
                        TryCoder.of(PubsubMessageWithAttributesCoder.of())));
        PAssert.that(failureCollection).empty();
        pipeline.run();
    }

    @Test
    public void splitWhenFailure() {
        val successTag = new TupleTag<PubsubMessage>() {};
        val failureTag = new TupleTag<Tuple2<Map<String, String>, Try<PubsubMessage>>>() {};
        val pCollectionTuple = pipeline
                .apply(Create.ofProvider(
                        ValueProvider.StaticValueProvider.of(
                                Tuple.of(getItemFullFeedRow(), Try.failure(new ProcessingException(OTHER)))
                        ), Tuple2Coder.of(MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()),
                                TryCoder.of(PubsubMessageWithAttributesCoder.of()))))
                .apply(GcsToPubSubStreamingPipeline.split(successTag, failureTag));
        // success
        val successCollection = pCollectionTuple.get(successTag).setCoder(PubsubMessageWithAttributesCoder.of());
        PAssert.that(successCollection).empty();
        // failure
        val failureCollection = pCollectionTuple.get(failureTag)
                .setCoder(Tuple2Coder.of(MapCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()),
                        TryCoder.of(PubsubMessageWithAttributesCoder.of())));
        PAssert.that(failureCollection)
                .satisfies(failures -> matchAll(failures,
                        map(failureTuple -> failureTuple._2.failed().get(), is(instanceOf(RuntimeException.class)))));
        pipeline.run();
    }
}
