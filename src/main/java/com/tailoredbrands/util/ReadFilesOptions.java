package com.tailoredbrands.util;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

public interface ReadFilesOptions extends PipelineOptions {
    @Description("The file pattern to read records from (e.g. gs://bucket/file-*.csv)")
    @Validation.Required
    ValueProvider<String> getInputFilePattern();

    void setInputFilePattern(ValueProvider<String> value);

    @Description("Output file for the pipeline")
    @Default.String("gs://my-bucket/output")
    String getOutputFilePattern();

    void setOutputFilePattern(String output);
}
