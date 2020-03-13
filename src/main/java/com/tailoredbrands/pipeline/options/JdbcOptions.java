package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface JdbcOptions extends PipelineOptions {

    @Validation.Required
    String getDriver();

    void setDriver(String value);

    @Validation.Required
    String getUrl();

    void setUrl(String value);

    @Validation.Required
    String getUser();

    void setUser(String value);

    @Validation.Required
    String getPassword();

    void setPassword(String value);
}
