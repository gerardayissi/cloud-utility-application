package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;

import java.io.Serializable;

public class GcsToPubSubCounter implements Serializable {

    public final Counter gcsFilesRead;
    public final Counter csvRowsRead;
    public final Counter csvRowToObjectErrors;
    public final Counter objectToJsonErrors;
    public final Counter jsonToBase64Errors;
    public final Counter jsonToPubSubErrors;
    public final Counter pubsubMessagesWritten;
    public final Counter untypedErrors;
    public final Counter totalErrors;

    public GcsToPubSubCounter(String namespace) {
        gcsFilesRead = Metrics.counter(namespace, "gcs-files-read");
        csvRowsRead = Metrics.counter(namespace, "csv-rows-read");
        csvRowToObjectErrors = Metrics.counter(namespace, "csv-row-to-object-errors");
        objectToJsonErrors = Metrics.counter(namespace, "object-to-json-errors");
        jsonToBase64Errors = Metrics.counter(namespace, "json-to-base64-errors");
        jsonToPubSubErrors = Metrics.counter(namespace, "json-to-pubsub-errors");
        pubsubMessagesWritten = Metrics.counter(namespace, "pubsub-messages-written");
        untypedErrors = Metrics.counter(namespace, "untyped-errors");
        totalErrors = Metrics.counter(namespace, "total-errors");
    }
}
