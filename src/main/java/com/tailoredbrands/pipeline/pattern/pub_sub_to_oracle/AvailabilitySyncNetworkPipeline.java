package com.tailoredbrands.pipeline.pattern.pub_sub_to_oracle;

import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.PubSubToOracleOptions;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static com.tailoredbrands.pipeline.error.ErrorType.JSON_TO_OBJECT_CONVERSION_ERROR;
import static com.tailoredbrands.util.Peek.increment;
import static io.vavr.API.$;
import static io.vavr.API.Case;

/**
 * 4.12 Availability Sync - Network
 */
public class AvailabilitySyncNetworkPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilitySyncNetworkPipeline.class);

    public static void main(String[] args) {
        PubSubToOracleOptions options = PipelineOptionsFactory
            .fromArgs(args)
            .withValidation()
            .as(PubSubToOracleOptions.class);

        run(options);
    }

    public static PipelineResult run(PubSubToOracleOptions options) {
        val pipeline = Pipeline.create(options);
        val counter = AvailabilitySyncCounter.of(options.getBusinessInterface());

        pipeline
            .apply("Read from PubSub", PubsubIO.readStrings().fromSubscription(options.getInputPubsubSubscription()))
            .apply("Input record counter", increment(counter.recordsRead))
            .apply("Create object", toPubSubMessage())
            .apply("Extract by viewId", extractView())
            .apply("Accumulating", accumulating())
            .apply("Output record counter", increment(counter.recordsWrite))
            .apply("Persist", writeToOracle(options))
        ;

        return pipeline.run();
    }

    private static Window<KV<String, PubsubMessage>> accumulating() {
        return Window.<KV<String, PubsubMessage>>
            into(Sessions.withGapDuration(Duration.standardSeconds(60L)))
            .withAllowedLateness(Duration.ZERO)
            .triggering(Repeatedly
                .forever(AfterWatermark
                    .pastEndOfWindow()
                    .withEarlyFirings(AfterProcessingTime
                        .pastFirstElementInPane()
                        .plusDelayOf(Duration.standardSeconds(10L)))))
            .accumulatingFiredPanes();
    }

    private static MapElements<String, Tuple2<String, Try<PubsubMessage>>> toPubSubMessage() {
        return MapElements
            .into(new TypeDescriptor<Tuple2<String, Try<PubsubMessage>>>() {})
            .via(record -> new Tuple2<>(record, Try
                .of(() -> new PubsubMessage(JsonUtils.serializeToBytes(record), new HashMap<>()))
                .mapFailure(Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(JSON_TO_OBJECT_CONVERSION_ERROR, exc)))));
    }



    private static MapElements<Tuple2<String, Try<PubsubMessage>>, KV<String, PubsubMessage>> extractView() {
        return MapElements
            .into(new TypeDescriptor<KV<String, PubsubMessage>>() {})
            .via(t2 -> KV.of(t2._2.get().getAttribute("ViewId"), t2._2.get()));
    }

    private final static String INSERT_STATEMENT = "INSERT INTO table VALUES ()";

    private static JdbcIO.Write<KV<String, PubsubMessage>> writeToOracle(PubSubToOracleOptions options) {
        return JdbcIO.<KV<String, PubsubMessage>>write()
            .withDataSourceConfiguration(JdbcIO.DataSourceConfiguration.create(
                options.getDriver(),
                options.getUrl())
                .withUsername(options.getUser())
                .withPassword(options.getPassword()))
            .withStatement(INSERT_STATEMENT)
            .withPreparedStatementSetter((JdbcIO.PreparedStatementSetter<KV<String, PubsubMessage>>)
                (kv, statement) -> statement.setString(1, kv.getKey()));
    }

}
