package com.tailoredbrands.pipeline.error;

import com.tailoredbrands.pipeline.options.ErrorHandlingOptions;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static io.vavr.API.Tuple;

public class ErrorHandlingPipeline {

  private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlingPipeline.class);

  public static void main(String[] args) {
    ErrorHandlingOptions options = PipelineOptionsFactory
        .fromArgs(args)
        .withValidation()
        .as(ErrorHandlingOptions.class);
    run(options);
  }

  public static PipelineResult run(ErrorHandlingOptions options) {
    Pipeline pipeline = Pipeline.create(options);

    Counter total = Metrics.counter("error_handler", "total_errors_processed");

    Map<String, Tuple2<String, Integer>> tags =
        List
            .ofAll(Arrays.asList(options.getPatternToBucketMap().split(",")))
            .zipWithIndex()
            .toMap(p2b2i -> {
              val split = p2b2i._1.split("|");
              return Tuple(split[0], Tuple(split[1], p2b2i._2));
            });

    Map<Integer, String> buckets = tags.values().toMap(Tuple2::swap);

    PCollectionList<ErrorMessage> list = pipeline
        .apply("Read messages from deadletter Pub/Sub", PubsubIO.readMessagesWithAttributes().fromSubscription(options.getDeadletterPubsubSubscription()))
        .apply("Log and count errors", countAndLogErrors(total))
        .apply("Map to ErrorMessage",
            MapElements
                .into(new TypeDescriptor<ErrorMessage>() {
                })
                .via(pm -> JsonUtils.fromJsonNode(JsonUtils.deserialize(pm.getPayload()), ErrorMessage.class))
        )
        .apply("Partition by output bucket", Partition.of(
            tags.size() + 1,
            (elem, numPartitions) -> {
              val buck2index = tags.get(elem.getFullPatternId());
              LOG.info(
                  "Will write errors for type {} to bucket {}, partition number {}",
                  elem.getFullPatternId(),
                  buck2index.map(t -> t._1).getOrElse("default"),
                  buck2index.map(t -> t._2).getOrElse(tags.size())
              );
              return buck2index.map(Tuple2::_2).getOrElse(tags.size());
            }
            )
        );

    for (int i = 0; i < tags.size() + 1; i++) {
      list
          .get(i)
          .apply("Map to strings", MapElements.into(TypeDescriptor.of(String.class)).via(ErrorMessage::getRawMessage))
          .apply("Window", Window.into(FixedWindows.of(Duration.standardSeconds(5L))))
          .apply("Write to GCS", TextIO.write().to(buckets.getOrElse(i, options.getDefaultBucket())).withWindowedWrites().withNumShards(1));
    }
    return pipeline.run();
  }

  private static Peek<PubsubMessage> countAndLogErrors(Counter counter) {
    return Peek.each(pubsubMessage -> {
      counter.inc();
      LOG.info(JsonUtils.serializeToString(JsonUtils.deserialize(pubsubMessage.getPayload())));
    });
  }
}
