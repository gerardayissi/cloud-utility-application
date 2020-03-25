package com.tailoredbrands.pipeline.pattern.pub_sub_to_oracle;

import com.tailoredbrands.pipeline.options.PubSubToOracleOptions;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tailoredbrands.util.SecretUtils.resolveSecret;

/**
 * This pipeline ingests incoming data from a Cloud Pub/Sub topic and
 * optputs the data into Oracle database.
 */
public class PubSubToOracleStreamingPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(PubSubToOracleStreamingPipeline.class);

    public static void main(String[] args) {
        PubSubToOracleOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(PubSubToOracleOptions.class);
        resolveSecrets(options);
        run(options);
    }

    private static void resolveSecrets(PubSubToOracleOptions options) {
        val project = options.as(GcpOptions.class).getProject();
        options.setUser(resolveSecret(project, options.getUser()));
        options.setPassword(resolveSecret(project, options.getPassword()));
    }

    public static PipelineResult run(PubSubToOracleOptions options) {
        val pipeline = Pipeline.create(options);
        pipeline
                .apply("Read from PubSub", PubsubIO.readStrings().fromSubscription(options.getInputPubsubSubscription()))
                .apply("Write to Oracle", writeToOracle(options));

        return pipeline.run();
    }

    private static JdbcIO.Write<String> writeToOracle(PubSubToOracleOptions options) {
        return JdbcIO.<String>write()
                .withDataSourceConfiguration(JdbcIO.DataSourceConfiguration.create(
                        options.getDriver(), options.getUrl())
                        .withUsername(options.getUser())
                        .withPassword(options.getPassword()))
                .withStatement("upsert into test_schema.test_table values(?)")
                .withPreparedStatementSetter((JdbcIO.PreparedStatementSetter<String>)
                        (event, query) -> query.setString(1, event));
    }
}
