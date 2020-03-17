package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Validation;

public interface JmsToPubSubOptions extends JmsOptions, PubSubOptions {
    @Validation.Required
    String getJmsToPubsubPipelineType();

    void setJmsToPubsubPipelineType(String value);
}
