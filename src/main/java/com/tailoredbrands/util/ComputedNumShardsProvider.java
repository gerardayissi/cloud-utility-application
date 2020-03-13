package com.tailoredbrands.util;

import io.vavr.collection.List;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;

import java.io.Serializable;

/**
 * Value provider to specify at runtime number of shards (number of parallel outputs, i.e. for FileIO).
 * If dynamic computing is enabled - calculate number of shards by multiplying max number of workers
 * (or min number of workers if not provided) by number of CPUs per workers.
 * <p/>
 * Number of min or max workers and machine type options are mandatory for each DataFlow and are
 * provided by TemplatesDeployer. For convenience we use
 * {@link ComputedNumShardsOptions} to access them.
 * <p/>
 * Default numShards if specified via separate option because different templates might need
 * different default number of shards, but we cannot override @Default value for option
 * inherited from parent Options interface
 */
public class ComputedNumShardsProvider implements ValueProvider<Integer>, Serializable {

    /**
     * Default numShards (to use when {@link #enabled} is false)
     */
    private final ValueProvider<Integer> defaultNumShards;
    /**
     * Flag to indicate whether to use computedNumShards
     */
    private final ValueProvider<Boolean> enabled;
    /**
     * numShards computed from numWorkers * numCPUs
     */
    private final int computedNumShards;

    private ComputedNumShardsProvider(ValueProvider<Integer> defaultNumShards, ValueProvider<Boolean> enabled, int computed) {
        this.defaultNumShards = defaultNumShards;
        this.enabled = enabled;
        this.computedNumShards = computed;
    }

    public static ComputedNumShardsProvider of(ComputedNumShardsOptions opts) {
        return new ComputedNumShardsProvider(StaticValueProvider.of(1), opts.getComputeNumShards(), computeNumShards(opts));
    }

    public ComputedNumShardsProvider withDefault(ValueProvider<Integer> defaultNumShards) {
        return new ComputedNumShardsProvider(defaultNumShards, enabled, computedNumShards);
    }

    @Override
    public Integer get() {
        return enabled.isAccessible() && enabled.get() ? computedNumShards : defaultNumShards.get();
    }

    @Override
    public boolean isAccessible() {
        return enabled.isAccessible() || defaultNumShards.isAccessible();
    }

    /**
     * Compute number of shards by multiplying max number of workers
     * (or min number of workers if not specified) by amount of CPUs per worker:
     * {@code num CPUs * max (or min) num Workers}.
     *
     * @return computed number of shards
     * @throws RuntimeException when required options were not specified
     */
    private static int computeNumShards(DataflowPipelineOptions options) {
        int workers = options.getMaxNumWorkers() > 0
                ? options.getMaxNumWorkers()
                : options.getNumWorkers();
        int cpus = Integer.parseInt(List.of(options.getWorkerMachineType().split("-")).last());
        return workers * cpus;
    }
}
