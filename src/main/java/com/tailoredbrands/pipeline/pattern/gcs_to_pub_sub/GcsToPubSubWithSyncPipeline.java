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

        PCollectionList<Tuple2<FileWithMeta, List<Try<JsonNode>>>> pcs = pipeline

            .apply("Match Files on GCS", FileIO.match()
                .filepattern(options.getInputFilePattern())
                .continuously(Duration.standardSeconds(options.getDurationSeconds()), Watch.Growth.never()))
            .apply("Read Files from GCS", FileIO.readMatches())
            .apply("Count Files", increment(counter.gcsFilesRead))
            .apply("Parse Files to Rows", ParDo.of(new CsvFileWithMetaFn()))
            .apply("Count Files after reading", increment(counter.gcsFilesRead))
            .apply("Process Business Interface", GCsToPubSubWithSyncProcessorFactory.from(options));

        // StartSync
        runStartSync(pcs, options, counter, successTag, failureTag);
//         process SyncDetail
        runSyncDetail(pcs, options, counter, successTag, failureTag);
        // End Sync
        runEndSync(pcs, options, counter, successTag, failureTag);

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
        TupleTag<Tuple2<FileWithMeta, PubsubMessage>> success,
        TupleTag<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> failure) {

        return ParDo.of(new DoFn<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>, Tuple2<FileWithMeta, PubsubMessage>>() {
            @ProcessElement
            public void processElement(ProcessContext context) {
                val element = context.element();
                element.map2(list -> {
                    list.forEach(pubsubMessage -> {
                        if (pubsubMessage.isSuccess()) {
                            context.output(new Tuple2(element._1, pubsubMessage.get()));
                        } else {
                            context.output(failure, element);
                        }
                    });
                    return null;
                });
            }
        }).withOutputTags(success, TupleTagList.of(failure));
    }

    private static Peek<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> logFailures(GcsToPubSubCounter counter) {
        return Peek.each(failure -> {
            ProcessingException err = (ProcessingException) failure._2.get(0).failed().get();
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

//    static MapElements<Tuple2<FileWithMeta, Try<PubsubMessage>>, Tuple2<FileWithMeta, Try<PubsubMessage>>> processFailure() {
//        return MapElements
//            .into(new TypeDescriptor<Tuple2<FileWithMeta, Try<PubsubMessage>>>() {
//            })
//            .via(tuple -> tuple.map2(
//                maybeJson -> maybeJson.map(jsonNode -> new PubsubMessage(JsonUtils.serializeToBytes(jsonNode), new HashMap<>()))
//                    .mapFailure(Case($(e -> !(e instanceof ProcessingException)),
//                        exc -> new ProcessingException(JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR, exc))))
//            );
//    }

    private static Peek<PubsubMessage> countAndLogOutbound(Counter counter) {
        return Peek.each(pubsubMessage -> {
            counter.inc();
            LOG.info(new String(pubsubMessage.getPayload()));
        });
    }

    private static void runStartSync(PCollectionList<Tuple2<FileWithMeta, List<Try<JsonNode>>>> pcs,
                                     GcsToPubSubOptions options,
                                     GcsToPubSubCounter counter,
                                     TupleTag<Tuple2<FileWithMeta, PubsubMessage>> successTag,
                                     TupleTag<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> failureTag) {

        val startSync = pcs.get(0)
            .apply("Convert Json To PubSubMessage", toPubSubMessage())
            .apply("Success | Failure", split(successTag, failureTag));

        startSync
            .get(successTag)
            .setCoder(Tuple2Coder.of(SerializableCoder.of(FileWithMeta.class), PubsubMessageWithAttributesCoder.of()))
            .apply("Map to PubSubMessage",
                MapElements
                    .into(new TypeDescriptor<PubsubMessage>() {
                    })
                    .via(tuple2 -> tuple2._2)
            )
            .setCoder(PubsubMessageWithAttributesCoder.of())
            .apply("Count and log messages to PubSub", countAndLogOutbound(counter.pubsubMessagesWritten))
            .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        startSync
            .get(failureTag)
            .apply("Log & Count Failures", logFailures(counter));
    }

    private static void runSyncDetail(PCollectionList<Tuple2<FileWithMeta, List<Try<JsonNode>>>> pcs,
                                      GcsToPubSubOptions options,
                                      GcsToPubSubCounter counter,
                                      TupleTag<Tuple2<FileWithMeta, PubsubMessage>> successTag,
                                      TupleTag<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> failureTag) {
        val syncDetail = pcs.get(1)
            .apply("Convert Json To PubSubMessage", toPubSubMessage())
            .apply("Success | Failure", split(successTag, failureTag));

        syncDetail
            .get(successTag)
            .setCoder(Tuple2Coder.of(SerializableCoder.of(FileWithMeta.class), PubsubMessageWithAttributesCoder.of()))
            .apply("Map to PubSubMessage",
                MapElements
                    .into(new TypeDescriptor<PubsubMessage>() {
                    })
                    .via(tuple2 -> tuple2._2)
            )
            .setCoder(PubsubMessageWithAttributesCoder.of())
            .apply("Count and log messages to PubSub", countAndLogOutbound(counter.pubsubMessagesWritten))
            .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        syncDetail
            .get(failureTag)
            .apply("Log & Count Failures", logFailures(counter));
    }

    private static void runEndSync(PCollectionList<Tuple2<FileWithMeta, List<Try<JsonNode>>>> pcs,
                                   GcsToPubSubOptions options,
                                   GcsToPubSubCounter counter,
                                   TupleTag<Tuple2<FileWithMeta, PubsubMessage>> success,
                                   TupleTag<Tuple2<FileWithMeta, List<Try<PubsubMessage>>>> failureTag) {

        val endSync = pcs.get(2)
            .apply("Convert Json To PubSubMessage", toPubSubMessage())
            .apply("Success | Failure", split(success, failureTag));

        endSync
            .get(success)
            .setCoder(Tuple2Coder.of(SerializableCoder.of(FileWithMeta.class), PubsubMessageWithAttributesCoder.of()))
            .apply("Map to PubSubMessage",
                MapElements
                    .into(new TypeDescriptor<PubsubMessage>() {
                    })
                    .via(tuple2 -> tuple2._2)
            )
            .setCoder(PubsubMessageWithAttributesCoder.of())
            .apply("Count and log messages to PubSub", countAndLogOutbound(counter.pubsubMessagesWritten))
            .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        endSync
            .get(success)
            .apply("Map to processed file",
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
    }
}
