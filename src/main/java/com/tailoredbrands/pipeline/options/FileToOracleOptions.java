package com.tailoredbrands.pipeline.options;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface FileToOracleOptions extends BusinessInterfaceOptions, JdbcOptions, GcsOptions, DataflowPipelineOptions {

    @Validation.Required
    String getTable();

    void setTable(String value);

}