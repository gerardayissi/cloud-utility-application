package com.tailoredbrands.pipeline.error;

import com.tailoredbrands.pipeline.options.ErrorHandlingOptions;
import com.tailoredbrands.util.Peek;
import com.tailoredbrands.util.json.JsonUtils;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Contextful;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    pipeline
        .apply("Read messages from deadletter Pub/Sub", PubsubIO.readMessagesWithAttributes().fromSubscription(options.getDeadletterPubsubSubscription()))
        .apply("Log and count errors", countAndLogErrors(total))
        .apply("Map to bucket and raw message",
            MapElements
                .into(new TypeDescriptor<KV<String, String>>() {
                })
                .via(pm -> {
                  val em = JsonUtils.fromJsonNode(JsonUtils.deserialize(pm.getPayload()), ErrorMessage.class);
                  return KV.of(em.getFullPatternId(), em.getRawMessage());
                })
        )
        .apply("Window", Window.into(FixedWindows.of(Duration.standardSeconds(5L))))
        .apply(
            "Write to GCS",
            FileIO.<String, KV<String, String>>writeDynamic()
                .via(Contextful.fn(KV::getValue), TextIO.sink())
                .by(KV::getKey)
                .withDestinationCoder(StringUtf8Coder.of())
                .to(options.getBucket())
                .withNaming(pattern -> FileIO.Write.defaultNaming(pattern + "/", ".txt"))
                .withNumShards(1)
            );
    return pipeline.run();
  }

  private static Peek<PubsubMessage> countAndLogErrors(Counter counter) {
    return Peek.each(pubsubMessage -> {
      counter.inc();
      LOG.info(JsonUtils.serializeToString(JsonUtils.deserialize(pubsubMessage.getPayload())));
    });
  }
}
