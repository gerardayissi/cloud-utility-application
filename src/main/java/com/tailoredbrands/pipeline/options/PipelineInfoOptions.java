package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface PipelineInfoOptions extends PipelineOptions {
  @Validation.Required
  @Description("The full name of the pattern, e.g. create_order_ecom")
  String getPatternFullName();

  void setPatternFullName(String value);

  @Description("TB user")
  String getTbUser();

  void setTbUser(String value);
}
