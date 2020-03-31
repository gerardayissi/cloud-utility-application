package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.tailoredbrands.business_interface.item_full_feed.ItemFullFeedProcessFileFn;
import com.tailoredbrands.pipeline.options.GcsToPubSubOptions;
import lombok.val;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Watch;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import static com.tailoredbrands.util.Peek.increment;
import static com.tailoredbrands.util.SecretUtils.resolveSecret;

/**
 * The {@code GcsToPubSubStreamingPipeline} takes incoming data from
 * Cloud Storage files and publishes it to Cloud Pub/Sub. The pipeline reads each
 * file row-by-row and publishes each record as a string message.
 */
public class GcsToPubSubStreamingPipeline {

    private static final Logger LOG = LoggerFactory.getLogger(GcsToPubSubStreamingPipeline.class);

    public static void main(String[] args) throws Exception {
        GcsToPubSubOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(GcsToPubSubOptions.class);
        run(options);
    }

    public static PipelineResult run(GcsToPubSubOptions options) throws Exception {
        val pipeline = Pipeline.create(options);
        val counter = new GcsToPubSubCounter(options.getBusinessInterface());
        val project = options.as(GcpOptions.class).getProject();
        val MAOCredentials = ServiceAccountCredentials.fromStream(new ByteArrayInputStream(
                resolveSecret(project, "MAO-PubSub-RW-Credentials").getBytes()));
        pipeline
                .apply("Match Files on GCS", FileIO.match()
                        .filepattern(options.getInputFilePattern())
                        .continuously(Duration.standardSeconds(60), Watch.Growth.never()))

                .apply("Read Files from GCS", FileIO.readMatches())
                .apply("Count Files", increment(counter.gcsFilesRead))

                .apply("Process File", ParDo.of(new ItemFullFeedProcessFileFn(options, counter, MAOCredentials)));

        return pipeline.run();
    }
}
