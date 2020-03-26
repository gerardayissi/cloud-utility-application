package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;

public interface PubSubOptions extends PipelineOptions {
    @Description("Pub/Sub subscription to read the input from. " +
            "Should be in format: projects/<project>/subscriptions/<subscription>")
    ValueProvider<String> getInputPubsubSubscription();

    void setInputPubsubSubscription(ValueProvider<String> value);

    @Description("Pub/Sub topic to write output to. " +
            "Should be in format: projects/<project>/topics/<topic>")
    ValueProvider<String> getOutputPubsubTopic();

    void setOutputPubsubTopic(ValueProvider<String> value);

    @Description("Pub/Sub list of output topics to be published " +
            "Should be in format: projects/<project>/topics/<topic1>,projects/<project>/topics/<topic2>")
    ValueProvider<String> getOutputTopics();

    void setOutputTopics(ValueProvider<String> value);

    @Description("Deadletter pubsub topic")
    ValueProvider<String> getDeadletterPubsubTopic();

    void setDeadletterPubsubTopic(ValueProvider<String> value);
}
