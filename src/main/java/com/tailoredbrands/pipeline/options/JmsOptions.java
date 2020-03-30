package com.tailoredbrands.pipeline.options;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface JmsOptions extends PipelineOptions {
    @Default.String("mqhatstsubcorp.tmw.com")
    String getJmsServerUrl();

    void setJmsServerUrl(String value);

    String getJmsCredentialsSecret();

    void setJmsCredentialsSecret(String value);

    @Validation.Required
    @Default.String("tibco")
    String getJmsProvider();
    void setJmsProvider(String value);

    @Validation.Required
    @Default.String("POS.TEST.Q")
    String getJmsQueue();

    void setJmsQueue(String value);

    @Validation.Required
    @Default.String("")
    String getQueueManager();

    void setQueueManager(String value);

    @Validation.Required
    @Default.Integer(2024)
    int getPort();

    void setPort(int value);

    @Validation.Required
    @Default.String("ESBCRS.LB.SVRCONN")
    String getChannel();

    void setChannel(String value);
}
