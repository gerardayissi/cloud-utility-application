package com.tailoredbrands.pipeline.pattern.oracle_to_pub_sub;

import com.tailoredbrands.pipeline.options.OracleToPubSubOptions;
import com.tailoredbrands.util.json.JsonUtils;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static com.tailoredbrands.util.SecretUtils.resolveSecret;

/**
 * The {@code OracleToPubSubBatchPipeline} takes incoming data from
 * Oracle database and publishes it to Cloud Pub/Sub. The pipeline reads the
 * table row-by-row and publishes each record as a string message.
 */
public class OracleToPubSubBatchPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(OracleToPubSubBatchPipeline.class);

    public static void main(String[] args) {
        OracleToPubSubOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(OracleToPubSubOptions.class);
        run(options);
    }

    private static void resolveSecrets(OracleToPubSubOptions options) {
        val project = options.as(GcpOptions.class).getProject();
        options.setUser(resolveSecret(project, options.getUser()));
        options.setPassword(resolveSecret(project, options.getPassword()));
    }

    public static PipelineResult run(OracleToPubSubOptions options) {
        val pipeline = Pipeline.create(options);
        resolveSecrets(options);
        pipeline
                .apply("Read from Oracle", readFromOracle(options))
                .apply("Convert to PubSub Message", toPubSubMessage())
                .apply("Write Messages to Pubsub", PubsubIO.writeMessages().to(options.getOutputPubsubTopic()));

        return pipeline.run();
    }

    private static JdbcIO.Read<String> readFromOracle(OracleToPubSubOptions options) {
        return JdbcIO.<String>read()
                .withDataSourceConfiguration(JdbcIO.DataSourceConfiguration.create(
                        options.getDriver(), options.getUrl())
                        .withUsername(options.getUser())
                        .withPassword(options.getPassword()))
                .withQuery("select username as schema_name\n" +
                        "from sys.all_users\n" +
                        "order by username")
                .withCoder(StringUtf8Coder.of())
                .withRowMapper((JdbcIO.RowMapper<String>) rs -> {
                    val row = new HashMap<String, Object>();
                    val columnCount = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    }
                    return JsonUtils.serializeObject(row);
                });
    }

    static MapElements<String, PubsubMessage> toPubSubMessage() {
        return MapElements
                .into(new TypeDescriptor<PubsubMessage>() {
                })
                .via(row -> new PubsubMessage(row.getBytes(), new HashMap<>()));
    }
}
