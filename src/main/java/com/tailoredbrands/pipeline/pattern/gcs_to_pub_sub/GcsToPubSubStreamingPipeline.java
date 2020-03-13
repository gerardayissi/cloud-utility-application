package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.google.cloud.hadoop.util.AbstractGoogleAsyncWriteChannel;
import com.tailoredbrands.util.DurationUtils;
import com.tailoredbrands.util.ReadFilesOptions;
import org.apache.beam.runners.dataflow.options.DataflowWorkerLoggingOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;

public class GcsToPubSubStreamingPipeline {

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

        DataflowWorkerLoggingOptions loggingOptions = options.as(DataflowWorkerLoggingOptions.class);
        loggingOptions.setWorkerLogLevelOverrides(
                new DataflowWorkerLoggingOptions.WorkerLogLevelOverrides().addOverrideForClass(AbstractGoogleAsyncWriteChannel.class,
                        DataflowWorkerLoggingOptions.Level.OFF));
        run(options);
    }

    /**
     * Runs the pipeline with the supplied options.
     *
     * @param options The execution parameters to the pipeline.
     * @return The result of the pipeline execution.
     */
    private static PipelineResult run(Options options) {
        // Create the pipeline
        Pipeline pipeline = Pipeline.create(options);
        /*
         * Steps:
         *   1) Read file data from GCS
         *   2) Window the messages into minute intervals specified by the executor.
         *   3) Output the windowed data to PubSub
         */
        pipeline
                .apply("Read CSV Files", TextIO.read().from(options.getInputFilePattern()))
                .apply(options.getWindowDuration() + " Window",
                        Window.into(FixedWindows.of(DurationUtils.parseDuration(options.getWindowDuration()))))
                .apply("Write to PubSub topic", PubsubIO.writeStrings().to(options.getOutputTopic()));

        // Execute the pipeline and return the result.
        return pipeline.run();
    }

    public interface Options extends PipelineOptions, StreamingOptions, ReadFilesOptions {

        @Description("The window duration in which data will be written. Defaults to 5 minutes. " + DurationUtils.USAGE)
        @Default.String("5m")
        String getWindowDuration();

        void setWindowDuration(String value);

        @Description("The name of the topic which data should be published to. "
                + "The name should be in the format of projects/<project-id>/topics/<topic-name>.")
        @Validation.Required
        ValueProvider<String> getOutputTopic();

        void setOutputTopic(ValueProvider<String> value);
    }
}
