package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.function.CsvFileWithMetaFn;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.FileWithMeta;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesCoder;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.*;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static com.tailoredbrands.pipeline.error.ErrorType.*;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.*;
import static java.lang.String.format;

public class GcsToPubSubWithSyncPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(GcsToPubSubWithSyncPipeline.class);

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
        val processedTag = new TupleTag<Tuple2<FileWithMeta, Try<PubsubMessage>>>() {
        };
        val failureTag = new TupleTag<Tuple2<FileWithMeta, Try<PubsubMessage>>>() {
        };

        PCollectionList<Tuple2<FileWithMeta, Try<JsonNode>>> pcl = pipeline

            .apply("Match Files on GCS", FileIO.match()
                .filepattern(options.getInputFilePattern())
                .continuously(Duration.standardSeconds(options.getDurationSeconds()), Watch.Growth.never()))
            .apply("Read Files from GCS", FileIO.readMatches())
            .apply("Count Files", increment(counter.gcsFilesRead))
            .apply("Parse Files to Rows", ParDo.of(new CsvFileWithMetaFn()))
            .apply("Count Rows", increment(counter.csvRowsRead))
            .apply("Process Business Interface", GCsToPubSubWithSyncProcessorFactory.from(options));

        io.vavr.collection.List.ofAll(pcl.getAll())
            .forEach(pc -> {
                    val processed = pc
                        .apply("Convert Json To PubSubMessage", toPubSubMessage())
                        .apply("Success | Failure", split(processedTag, failureTag));

                    processed
                        .get(processedTag)
                        .apply("Map to PubSubMessage",
                            MapElements
                                .into(new TypeDescriptor<PubsubMessage>() {
                                })
                                .via(tuple2 -> tuple2._2.get()))
                        .setCoder(PubsubMessageWithAttributesCoder.of())
                        .apply("Count Messages to PubSub", increment(counter.pubsubMessagesWritten))
                        .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

                    processed
                        .get(processedTag)
                        .apply("Map to bucket and raw message",
                            MapElements
                                .into(new TypeDescriptor<KV<String, String>>() {
                                })
                                .via(tuple2 -> KV.of(tuple2._1.getSourceName(), tuple2._1.getFileContent())))
                        .apply("Window", Window.into(FixedWindows.of(Duration.standardSeconds(5L))))

                        .apply("Write File to processed bucket",
                            FileIO.<String, KV<String, String>>writeDynamic()
                                .by(KV::getKey)
                                .withDestinationCoder(StringUtf8Coder.of())
                                .via(Contextful.fn(KV::getValue), TextIO.sink())
                                .to(options.getProcessedBucket())
                                .withNaming(key -> FileIO.Write.defaultNaming(key.replaceAll(".csv", ""), ".csv"))
                                .withNumShards(1)
                        );

                    processed
                        .get(failureTag)
                        .apply("Log & Count Failures", logAndCountFailures(counter));
                }
            );

        return pipeline.run();
    }

    @SuppressWarnings("unchecked")
    static MapElements<Tuple2<FileWithMeta, Try<JsonNode>>, Tuple2<FileWithMeta, Try<PubsubMessage>>> toPubSubMessage() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<FileWithMeta, Try<PubsubMessage>>>() {
            })
            .via(tuple -> tuple.map2(
                maybeJson -> maybeJson.map(jsonNode -> new PubsubMessage(JsonUtils.serializeToBytes(jsonNode), new HashMap<>()))
                    .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR, exc))))
            );
    }

    static ParDo.MultiOutput<Tuple2<FileWithMeta, Try<PubsubMessage>>, Tuple2<FileWithMeta, Try<PubsubMessage>>> split(
        TupleTag<Tuple2<FileWithMeta, Try<PubsubMessage>>> success,
        TupleTag<Tuple2<FileWithMeta, Try<PubsubMessage>>> failure) {

        return ParDo.of(new DoFn<Tuple2<FileWithMeta, Try<PubsubMessage>>, Tuple2<FileWithMeta, Try<PubsubMessage>>>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                val element = context.element();
                if (element._2.isSuccess()) {
                    context.output(new Tuple2(element._1, element._2));
                } else {
                    context.output(failure, element);
                }
            }
        }).withOutputTags(success, TupleTagList.of(failure));
    }

    private static Peek<Tuple2<FileWithMeta, Try<PubsubMessage>>> logAndCountFailures(GcsToPubSubCounter counter) {
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
