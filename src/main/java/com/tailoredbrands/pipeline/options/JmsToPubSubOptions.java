package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Validation;

public interface JmsToPubSubOptions extends JmsOptions, PubSubOptions {
    @Validation.Required
    @Default.String("websphere")
    String getJmsToPubsubPipelineType();

    void setJmsToPubsubPipelineType(String value);
}
