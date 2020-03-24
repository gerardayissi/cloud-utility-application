package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.function.CsvFileWithMetadataFn;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.FileRowMetadata;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesCoder;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Watch;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import static com.tailoredbrands.pipeline.error.ErrorType.*;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.*;
import static java.lang.String.format;

public class GcsToPubSubWithSyncPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(GcsToPubSubBatchPipeline.class);

    public static void main(String[] args) {
        GcsToPubSubOptions options = PipelineOptionsFactory
            .fromArgs(args)
            .withValidation()
            .as(GcsToPubSubOptions.class);
        run(options);
    }

    public static PipelineResult run(GcsToPubSubOptions options) {
        val pipeline = Pipeline.create(options);
        val counter = new GcsToPubSubCounter(options.getBusinessInterface());
        val successTag = new TupleTag<PubsubMessage>() {
        };
        val failureTag = new TupleTag<Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>>() {
        };

        PCollectionList<Tuple2<List<FileRowMetadata>, Try<JsonNode>>> pcl = pipeline

            .apply("Match Files on GCS", FileIO.match()
                .filepattern(options.getInputFilePattern())
                .continuously(Duration.standardSeconds(options.getDurationSeconds()), Watch.Growth.never()))
            .apply("Read Files from GCS", FileIO.readMatches())
            .apply("Count Files", increment(counter.gcsFilesRead))
            .apply("Parse Files to Rows", ParDo.of(new CsvFileWithMetadataFn()))
            .apply("Count Rows", increment(counter.csvRowsRead))
            .apply("Process Business Interface", GCsToPubSubWithSyncProcessorFactory.from(options));

        io.vavr.collection.List.ofAll(pcl.getAll())
            .map(pc -> pc
                .apply("Convert Json To PubSubMessage", toPubSubMessage())
                .apply("Success | Failure", split(successTag, failureTag)))

            .map(successPc -> successPc
                .get(successTag)
                .setCoder(PubsubMessageWithAttributesCoder.of())
                .apply("Count Messages to PubSub", increment(counter.pubsubMessagesWritten))
                .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic())));

//        processed
//            .apply("Convert Json To PubSub Message", toPubSubMessage());
//            .apply("Success | Failure", split(successTag, failureTag));
//
//        processed
//            .get(failureTag)
//            .apply("Log & Count Failures", logAndCountFailures(counter));
//
//        processed
//            .get(successTag)
//            .setCoder(PubsubMessageWithAttributesCoder.of())
//            .apply("Count Messages to PubSub", increment(counter.pubsubMessagesWritten))
//            .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        return pipeline.run();
    }

    static class PrintResultsFn extends DoFn<Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>, Void> {
        @ProcessElement
        public void processElement(@Element Tuple2<List<FileRowMetadata>, Try<PubsubMessage>> words) {
            LOG.info(words.toString());
        }
    }

    @SuppressWarnings("unchecked")
    static MapElements<Tuple2<List<FileRowMetadata>, Try<JsonNode>>, Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>> toPubSubMessage() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>>() {
            })
            .via(tuple -> tuple.map2(
                maybeJson -> maybeJson.map(jsonNode -> new PubsubMessage(JsonUtils.serializeToBytes(jsonNode), new HashMap<>()))
                    .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR, exc))))
            );
    }

    static ParDo.MultiOutput<Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>, PubsubMessage> split(
        TupleTag<PubsubMessage> success,
        TupleTag<Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>> failure) {

        return ParDo.of(new DoFn<Tuple2<List<FileRowMetadata>, Try<PubsubMessage>>, PubsubMessage>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                val element = context.element();
                if (element._2.isSuccess()) {
                    context.output(element._2.get());
                } else {
                    context.output(failure, element);
                }
            }
        }).withOutputTags(success, TupleTagList.of(failure));
    }

    private static Peek<Tuple2<FileRowMetadata, Try<PubsubMessage>>> logAndCountFailures(GcsToPubSubCounter counter) {
        return Peek.each(failure -> {
            ProcessingException err = (ProcessingException) failure._2.failed().get();
            val detailedCounter = Match(err.getType()).of(
                Case($(CSV_ROW_TO_OBJECT_CONVERSION_ERROR), counter.csvRowToObjectErrors),
                Case($(OBJECT_TO_JSON_CONVERSION_ERROR), counter.objectToJsonErrors),
                Case($(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR), counter.jsonToPubSubErrors),
                Case($(), counter.untypedErrors)
            );
            detailedCounter.inc();
            counter.totalErrors.inc();
            LOG.error(format("Failed to process Row: %s", failure._1), err);
        });
    }
}
