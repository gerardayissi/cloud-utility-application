package com.tailoredbrands.pipeline.pattern.pub_sub_to_gcs;

import com.google.cloud.hadoop.util.AbstractGoogleAsyncWriteChannel;
import com.tailoredbrands.util.ComputedNumShardsProvider;
import com.tailoredbrands.util.DatePartitionedFileNaming;
import com.tailoredbrands.util.DurationUtils;
import com.tailoredbrands.util.WriteFilesOptions;
import org.apache.beam.runners.dataflow.options.DataflowWorkerLoggingOptions;
import org.apache.beam.runners.dataflow.options.DataflowWorkerLoggingOptions.Level;
import org.apache.beam.runners.dataflow.options.DataflowWorkerLoggingOptions.WorkerLogLevelOverrides;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * This pipeline ingests incoming data from a Cloud Pub/Sub topic and
 * outputs the raw data into windowed files at the specified output
 * directory.
 *
 * <p> Example Usage:
 *
 * <pre>
 * gradle runDataFlow \
 * -PpipeName=com.tailored.dev.pipelines.${PIPELINE_NAME} \
 * -PpipeArgs=" \
 * --project=${PROJECT_ID} \
 * --stagingLocation=gs://${PROJECT_ID}/dataflow/pipelines/${PIPELINE_FOLDER}/staging \
 * --tempLocation=gs://${PROJECT_ID}/dataflow/pipelines/${PIPELINE_FOLDER}/temp \
 * --ioTempLocation=gs://${PROJECT_ID}/dataflow/pipelines/${PIPELINE_FOLDER}/io/app \
 * --runner=DataflowRunner \
 * --windowDuration=2m \
 * --numShards=1 \
 * --inputTopic=projects/${PROJECT_ID}/topics/example-topic \
 * --outputDirectory=gs://${PROJECT_ID}/temp/ \
 * </pre>
 * </p>
 */
public class PubSubToGcsStreamingPipeline {

    /**
     * Options supported by the pipeline.
     *
     * <p>Inherits standard configuration options.</p>
     */
    public interface Options extends PipelineOptions, StreamingOptions, WriteFilesOptions {

        @Description("The Cloud Pub/Sub topic to read from.")
        @Required
        ValueProvider<String> getInputSubscription();

        void setInputSubscription(ValueProvider<String> value);

        @Description("The maximum number of output shards produced when writing.")
        @Default.Integer(1)
        ValueProvider<Integer> getNumShards();

        void setNumShards(ValueProvider<Integer> value);

        @Description("The window duration in which data will be written. Defaults to 5 minutes. " + DurationUtils.USAGE)
        @Default.String("5m")
        String getWindowDuration();

        void setWindowDuration(String value);

        @Description("IO temp location")
        ValueProvider<String> getIoTempLocation();

        void setIoTempLocation(ValueProvider<String> value);
    }

    private static final Pattern REPLAY_PATTERN = Pattern.compile("\"_c\"\\s*:\\s*\"[0-9]+");

    /**
     * Main entry point for executing the pipeline.
     *
     * @param args The command-line arguments to the pipeline.
     */
    @SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
    public static void main(String[] args) {
        Options options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(Options.class);
        options.setStreaming(true);
        // NOTE: AbstractGoogleAsyncWriteChannel class in 1.x version
        // logs unnecessarily errors along with propagating them up
        // in 2.x version that is already fixed but dataflow does not support that yet
        // so we just suppress them
        DataflowWorkerLoggingOptions loggingOptions = options.as(DataflowWorkerLoggingOptions.class);
        loggingOptions.setWorkerLogLevelOverrides(
                new WorkerLogLevelOverrides().addOverrideForClass(AbstractGoogleAsyncWriteChannel.class, Level.OFF));
        run(options);
    }

    /**
     * Runs the pipeline with the supplied options.
     *
     * @param options The execution parameters to the pipeline.
     * @return The result of the pipeline execution.
     */
    public static PipelineResult run(Options options) {
        // Create the pipeline
        Pipeline pipeline = Pipeline.create(options);
        /*
         * Steps:
         *   0) Read string messages from PubSub
         *   1) Filter out replay messages
         *   2) Window the messages into minute intervals specified by the executor.
         *   3) Output the windowed files to GCS
         */
        pipeline
                .apply("Read PubSub Events", PubsubIO.readStrings().fromSubscription(options.getInputSubscription()))
                .apply("Filter Out Replays", Filter.by(messageString -> !REPLAY_PATTERN.matcher(messageString).find()))
                .apply(
                        options.getWindowDuration() + " Window",
                        Window.into(FixedWindows.of(DurationUtils.parseDuration(options.getWindowDuration()))))
                .apply(
                        "Write File(s)",
                        FileIO.<String>write()
                                .via(TextIO.sink())
                                .withTempDirectory(options.getIoTempLocation())
                                .withNumShards(ComputedNumShardsProvider.of(options)
                                        .withDefault(options.getNumShards()))
                                .to(options.getOutputDirectory())
                                .withNaming(DatePartitionedFileNaming.withWindowDate()
                                        .withPrefix(options.getOutputFilenamePrefix())
                                        .withSuffix(
                                                // compute and cache unique suffix for current run to reduce chance
                                                // of rewriting existing files (from the same window and pane) after job restart
                                                NestedValueProvider.of(
                                                        options.getOutputFilenameSuffix(),
                                                        PubSubToGcsStreamingPipeline::uniqueSuffix))));

        // Execute the pipeline and return the result.
        return pipeline.run();
    }

    private static String uniqueSuffix(String suffix) {
        return "-" + Instant.now().getEpochSecond() + "-" + suffix;
    }
}
