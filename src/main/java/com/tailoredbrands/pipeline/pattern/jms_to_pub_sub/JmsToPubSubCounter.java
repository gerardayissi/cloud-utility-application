package com.tailoredbrands.pipeline.pattern.jms_to_pub_sub;

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;

import java.io.Serializable;

public class JmsToPubSubCounter implements Serializable {
    public final Counter jmsRecordsRead;

    public final Counter jmsPayloadExtractionErrors;

    public final Counter pubsubMessagesWritten;

    public final Counter jmsRecordToObjectErrors;

    public final Counter objectToJsonErrors;

    public final Counter jsonToPubSubErrors;

    public final Counter totalErrors;

    public final Counter untypedErrors;

    public JmsToPubSubCounter(String namespace) {
        this.jmsRecordsRead = Metrics.counter(namespace, "jms-messages-read");
        this.pubsubMessagesWritten = Metrics.counter(namespace, "pubsub-messages-written");
        this.jmsRecordToObjectErrors = Metrics.counter(namespace, "jms-to-object-errors");
        this.objectToJsonErrors = Metrics.counter(namespace, "object-to-json-errors");
        this.jsonToPubSubErrors = Metrics.counter(namespace, "json-to-pubsub-errors");
        this.jmsPayloadExtractionErrors = Metrics.counter(namespace, "jms-payload-extraction-errors");
        this.totalErrors = Metrics.counter(namespace, "total-errors");
        this.untypedErrors = Metrics.counter(namespace, "untyped-errors");
    }
}
