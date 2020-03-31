package com.tailoredbrands.pipeline.pattern.pub_sub_to_oracle;

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;

import java.io.Serializable;

public class AvailabilitySyncCounter implements Serializable {
    public final Counter recordsRead;
    public final Counter recordsWrite;

    public final Counter totalErrors;
    public final Counter untypedErrors;

    protected AvailabilitySyncCounter(String namespace) {
        this.recordsRead = Metrics.counter(namespace, "records-read");
        this.recordsWrite = Metrics.counter(namespace, "records-write");
        this.totalErrors = Metrics.counter(namespace, "total-errors");
        this.untypedErrors = Metrics.counter(namespace, "untyped-errors");
    }

    public static AvailabilitySyncCounter of(String namespace) {
        return new AvailabilitySyncCounter(namespace);
    }
}
