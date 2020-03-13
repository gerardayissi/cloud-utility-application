package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface CsvOptions extends PipelineOptions {
    @Validation.Required
    @Description("Delimiter which separates columns in csv file")
    String getDelimiter();

    void setDelimiter(String value);
}