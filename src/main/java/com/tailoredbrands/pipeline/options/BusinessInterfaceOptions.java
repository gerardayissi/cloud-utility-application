package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface BusinessInterfaceOptions extends PipelineOptions {
    @Validation.Required
    @Description("The name of the actual business interface, ex: item_full_feed")
    String getBusinessInterface();

    void setBusinessInterface(String value);
}
