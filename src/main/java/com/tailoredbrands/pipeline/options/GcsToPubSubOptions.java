package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface GcsToPubSubOptions extends BusinessInterfaceOptions, CsvOptions, CustomGcsOptions, PubSubOptions {
    @Validation.Required
    @Description("The name of the User, ex: admin@tbi.com")
    String getUser();

    void setUser(String value);

    @Validation.Required
    @Description("The name of the Organization, ex: TMW")
    String getOrganization();

    void setOrganization(String value);
}