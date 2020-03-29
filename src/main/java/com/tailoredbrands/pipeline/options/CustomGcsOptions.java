package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.extensions.gcp.options.GcsOptions;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface CustomGcsOptions extends GcsOptions {
    @Validation.Required
    @Description("The file pattern to read records from (e.g. gs://bucket/file-*.csv)")
    String getInputFilePattern();

    void setInputFilePattern(String value);
}