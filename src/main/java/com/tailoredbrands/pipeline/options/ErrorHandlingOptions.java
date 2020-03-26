package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

public interface ErrorHandlingOptions extends PipelineOptions {
  @Validation.Required
  @Description("Pub/Sub subscription to read errors from")
  ValueProvider<String> getDeadletterPubsubSubscription();

  void setDeadletterPubsubSubscription(ValueProvider<String> value);

  @Validation.Required
  @Description("GCS bucket")
  String getBucket();

  void setBucket(String value);
}
