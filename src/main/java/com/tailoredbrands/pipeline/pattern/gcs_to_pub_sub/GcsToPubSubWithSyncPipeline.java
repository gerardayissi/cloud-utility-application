package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.function.CsvFileWithMetaFn;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import com.tailoredbrands.util.FileWithMeta;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessageWithAttributesCoder;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.*;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        val successTag = new TupleTag<Tuple2<FileWithMeta, PubsubMessage>>() {
        };
        val failureTag = new TupleTag<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>>() {
        };
        val toBucketThenPubSubTag = new TupleTag<Tuple2<FileWithMeta, PubsubMessage>>() {
        };
        val toPubSubTag = new TupleTag<PubsubMessage>() {
        };

        PCollectionTuple pc = pipeline

            .apply("Match Files on GCS", FileIO.match()
                .filepattern(options.getInputFilePattern())
                .continuously(Duration.standardSeconds(options.getDurationSeconds()), Watch.Growth.never()))
            .apply("Read Files from GCS", FileIO.readMatches())
            .apply("Count Files", increment(counter.gcsFilesToRead))
            .apply("Parse Files to Rows", ParDo.of(new CsvFileWithMetaFn()))
            .apply("Count Files after reading", increment(counter.gcsFilesRead))
            .apply("Process Business Interface", GCsToPubSubWithSyncProcessorFactory.from(options))
            .apply("Convert Json To PubSubMessage", toPubSubMessage())
            .apply("Checks errors", split(options.getErrorThreshold(), successTag, failureTag));

        // process success flow
        // splitting to pubsub and bucket
        PCollectionTuple pctSuccess = pc
            .get(successTag)
            .setCoder(Tuple2Coder.of(SerializableCoder.of(FileWithMeta.class), PubsubMessageWithAttributesCoder.of()))
            .apply("Split messages to PubSub | Bucket", splitSuccess(toBucketThenPubSubTag, toPubSubTag));

        pctSuccess
            .get(toPubSubTag)
            .setCoder(PubsubMessageWithAttributesCoder.of())
            .apply("Log and count messages to PubSub", countAndLogOutbound(counter.pubSubMessagesWritten))
            .apply("Write Messages to PubSub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        // write endSync msg
        pctSuccess
            .get(toBucketThenPubSubTag)
            .setCoder(Tuple2Coder.of(SerializableCoder.of(FileWithMeta.class), PubsubMessageWithAttributesCoder.of()))
            .apply("Map to PubSubMessage",
                MapElements
                    .into(new TypeDescriptor<PubsubMessage>() {
                    })
                    .via(tuple2 -> tuple2._2)
            )
            .setCoder(PubsubMessageWithAttributesCoder.of())
            .apply("Log and count End Sync message to PubSub", countAndLogOutbound(counter.pubSubEndSyncMessagesWritten))
            .apply("Write Messages to PubSub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        // move processed file after end sync
        pctSuccess
            .get(toBucketThenPubSubTag)
            .apply("Map to processed file",
                MapElements
                    .into(new TypeDescriptor<KV<String, String>>() {
                    })
                    .via(tuple2 -> KV.of(tuple2._1.getSourceName(), tuple2._1().getFileContent())))
            .apply("Window", Window.into(FixedWindows.of(Duration.standardSeconds(5L))))
            .apply("Count files to be creating", increment(counter.processedFilesCreated))
            .apply("Write Processed File into the bucket",
                FileIO.<String, KV<String, String>>writeDynamic()
                    .by(KV::getKey)
                    .withDestinationCoder(StringUtf8Coder.of())
                    .via(Contextful.fn(KV::getValue), TextIO.sink())
                    .to(options.getProcessedBucket())
                    .withNaming(key -> FileIO.Write.defaultNaming(key.replaceAll(".csv", ""), ".csv"))
                    .withNumShards(1)
            );

        // failure flow
        pc
            .get(failureTag)
            .apply("Map failure rows", mapFailure())
            .apply("Log and count errors", logFailures(counter))
            .apply("Map to file with failures",
                MapElements
                    .into(new TypeDescriptor<KV<String, String>>() {
                    })
                    .via(tuple2 -> KV.of(tuple2._1.getSourceName(), tuple2._1().getFileContent())))
            .apply("Window", Window.into(FixedWindows.of(Duration.standardSeconds(5L))))
            .apply("Count files to be creating", increment(counter.filesWithFailuresCreated))
            .apply("Write file with failures into the bucket",
                FileIO.<String, KV<String, String>>writeDynamic()
                    .by(KV::getKey)
                    .withDestinationCoder(StringUtf8Coder.of())
                    .via(Contextful.fn(KV::getValue), TextIO.sink())
                    .to(options.getFailureBucket())
                    .withNaming(key -> FileIO.Write.defaultNaming("Error_" + key.replaceAll(".csv", ""), ".csv"))
                    .withNumShards(1)
            );

        return pipeline.run();
    }

    @SuppressWarnings("unchecked")
    static MapElements<Tuple2<FileWithMeta, List<Try<JsonNode>>>, Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> toPubSubMessage() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>>() {
            })
            .via(tuple -> tuple
                .map2(list -> {
                        val res = new ArrayList<Try<PubsubMessage>>();
                        list.forEach(jsonNodeTry -> {
                            val pubSubOrElse = jsonNodeTry.map(jsonNode -> new PubsubMessage(JsonUtils.serializeToBytes(jsonNode), new HashMap<>()))
                                .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
                                    exc -> new ProcessingException(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR, exc)));
                            res.add(pubSubOrElse);
                        });
                        return res;
                    }
                ));
    }

    static ParDo.MultiOutput<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>, Tuple2<FileWithMeta, PubsubMessage>> split(
        Integer thresholdPercentage,
        TupleTag<Tuple2<FileWithMeta, PubsubMessage>> success,
        TupleTag<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> failure) {

        return ParDo.of(new DoFn<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>, Tuple2<FileWithMeta, PubsubMessage>>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                val element = context.element();
                val errorsCnt = element._2().stream().filter(failure -> failure.isFailure()).count();
                if (errorsCnt > 0) {
                    val errorsPercent = errorsCnt * 100 / element._1.getRecords().size();
                    if (errorsPercent > thresholdPercentage) {
                        // move all file into error file
                        context.output(failure, element);
                    }
                } else {
                    // success, move forward with all records
                    element.map2(list -> {
                        list.forEach(pubsubMessage -> {
                            if (pubsubMessage.isSuccess()) {
                                context.output(new Tuple2(element._1, pubsubMessage.get()));
                            }
                        });
                        return null;
                    });
                }
            }
        }).withOutputTags(success, TupleTagList.of(failure));
    }

    static ParDo.MultiOutput<Tuple2<FileWithMeta, PubsubMessage>, Tuple2<FileWithMeta, PubsubMessage>> splitSuccess(
        TupleTag<Tuple2<FileWithMeta, PubsubMessage>> toBucket,
        TupleTag<PubsubMessage> toPubSub) {

        return ParDo.of(new DoFn<Tuple2<FileWithMeta, PubsubMessage>, Tuple2<FileWithMeta, PubsubMessage>>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                val element = context.element();
                val message = JsonUtils.deserialize(element._2.getPayload());
                val messageType = message.get("messages").get(0).get("data").get("SyncSupplyEvent").get("TransactionTypeId").asText();
                if (messageType.equals("EndSync")) {
                    context.output(new Tuple2<>(element._1, element._2));
                } else context.output(toPubSub, element._2());
            }
        }).withOutputTags(toBucket, TupleTagList.of(toPubSub));
    }

    static ParDo.SingleOutput<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>, Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> mapFailure() {
        return ParDo.of(new DoFn<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>, Tuple2<FileWithMeta, List<Try<PubsubMessage>>>>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                val element = context.element();
                val messages = element._2;
                messages
                    .stream()
                    .filter(row -> row.isSuccess())
                    .forEach(msg -> {
                        val message = JsonUtils.deserialize(msg.get().getPayload());
                        val messageType = message.get("messages").get(0).get("data").get("SyncSupplyEvent").get("TransactionTypeId").asText();
                        if (messageType.equals("StartSync")) {
                            context.output(new Tuple2<>(element._1, element._2));
                        }
                    });

            }
        });
    }

    private static Peek<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> logFailures(GcsToPubSubCounter counter) {
        return Peek.each(failure -> {
            val errors = failure._2;
            errors
                .stream()
                .filter(row -> row.isFailure())
                .forEach(err -> {
                    ProcessingException exc = (ProcessingException) err.failed().get();
                    val detailedCounter = Match(exc.getType()).of(
                        Case($(CSV_ROW_TO_OBJECT_CONVERSION_ERROR), counter.csvRowToObjectErrors),
                        Case($(OBJECT_TO_JSON_CONVERSION_ERROR), counter.objectToJsonErrors),
                        Case($(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR), counter.jsonToPubSubErrors),
                        Case($(), counter.untypedErrors)
                    );
                    detailedCounter.inc();
                    counter.totalErrors.inc();
                    LOG.error(format("Failed to process Row: %s", failure._1), err);
                });
        });
    }

    private static Peek<PubsubMessage> countAndLogOutbound(Counter counter) {
        return Peek.each(pubsubMessage -> {
            counter.inc();
            LOG.info(new String(pubsubMessage.getPayload()));
        });
    }
}
