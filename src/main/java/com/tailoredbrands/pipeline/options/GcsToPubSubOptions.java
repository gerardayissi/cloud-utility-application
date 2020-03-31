package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface GcsToPubSubOptions extends BusinessInterfaceOptions, CsvOptions, CustomGcsOptions, PubSubOptions {
    @Validation.Required
    @Description("The name of the User, ex: admin@tbi.com")
    String getUser();

    void setUser(String value);

    @Description("The name of the Organization, ex: TMW")
    String getOrganization();

    void setOrganization(String value);

    @Description("The duration in seconds ex: 60")
    Long getDurationSeconds();

    void setDurationSeconds(Long value);

    @Description("The error threshold in percentage ex: 10")
    Integer getErrorThreshold();

    void setErrorThreshold(Integer value);

    @Description("Batch payload of no more than this size records ex: 300")
    Integer getBatchPayloadSize();

    void setBatchPayloadSize(Integer value);
}