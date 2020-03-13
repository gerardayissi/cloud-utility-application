package com.tailoredbrands.util;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

public interface WriteFilesOptions extends ComputedNumShardsOptions {

    @Description("The directory to output files to. Must end with a slash.")
    @Validation.Required
    ValueProvider<String> getOutputDirectory();

    void setOutputDirectory(ValueProvider<String> value);

    @Description("The filename prefix of the files to write to.")
    @Default.String("file")
    ValueProvider<String> getOutputFilenamePrefix();

    void setOutputFilenamePrefix(ValueProvider<String> value);

    @Description("The suffix of the files to write.")
    @Default.String(".txt")
    ValueProvider<String> getOutputFilenameSuffix();

    void setOutputFilenameSuffix(ValueProvider<String> value);
}
