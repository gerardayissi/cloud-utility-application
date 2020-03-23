package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface GcsOptions extends PipelineOptions {
    @Validation.Required
    @Description("The file pattern to read records from (e.g. gs://bucket/file-*.csv)")
    String getInputFilePattern();

    void setInputFilePattern(String value);

//    @Description("The file to read records from (e.g. gs://bucket/file-location.csv)")
//    String getGCSFilePath();
//
//    void setGCSFilePath(String value);

    @Description("The file to read records from (e.g. gs://bucket/file-location.csv)")
    String getErrorGCSFilePath();

    void setErrorGCSFilePath(String value);

    @Description("The file to read records from (e.g. gs://bucket/file-location.csv)")
    String getProcessedGCSFilePath();

    void setProcessedGCSFilePath(String value);
}