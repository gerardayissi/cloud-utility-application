package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.extensions.gcp.options.GcsOptions;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface CustomGcsOptions extends GcsOptions {
    @Validation.Required
    @Description("The file pattern to read records from (e.g. gs://bucket/file-*.csv)")
    String getInputFilePattern();

    void setInputFilePattern(String value);

    @Validation.Required
    @Description("The bucket where we need to move processed files")
    String getProcessedBucket();

    void setProcessedBucket(String value);

    @Description("The bucket where we need to move files with failures")
    String getFailureBucket();

    void setFailureBucket(String value);
}