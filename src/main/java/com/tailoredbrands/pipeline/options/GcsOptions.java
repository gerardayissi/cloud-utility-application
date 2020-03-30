package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface GcsOptions extends PipelineOptions {
    @Validation.Required
    @Description("The file pattern to read records from (e.g. gs://bucket/file-*.csv)")
    String getInputFilePattern();

    void setInputFilePattern(String value);

    @Validation.Required
    @Description("The bucket where we need to move processed files")
    String getProcessedBucket();

    void setProcessedBucket(String value);
}