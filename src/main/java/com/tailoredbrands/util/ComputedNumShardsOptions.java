package com.tailoredbrands.util;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.ValueProvider;

/**
 * Options to provide feature to compute dynamically number of shards.
 * Can be used with {@link ComputedNumShardsProvider}.
 * <p/>
 * Number of min or max workers and machine type options are mandatory for each DataFlow and are
 * provided by  and accessed via {@link DataflowPipelineOptions}
 * <p/>
 * Default numShards is not included because different templates might need
 * different default number of shards, but we cannot override @Default value for option
 * inherited from parent Options interface
 */
public interface ComputedNumShardsOptions extends DataflowPipelineOptions {

    @Description("Flag to compute numShards option from maxWorkers * numCPUs")
    @Default.Boolean(true)
    ValueProvider<Boolean> getComputeNumShards();

    void setComputeNumShards(ValueProvider<Boolean> value);
}
