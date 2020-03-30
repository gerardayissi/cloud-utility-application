package com.tailoredbrands.business_interface.store_inventory_full_feed;

import com.tailoredbrands.business_interface.store_inventory_full_feed.accumulators.StoreInventoryFullFeedAccum;
import com.tailoredbrands.generated.json.store_inventory_full_feed.SupplyDetail;
import com.tailoredbrands.util.FileWithMeta;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderRegistry;
import org.apache.beam.sdk.coders.ListCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.transforms.Combine;

import java.util.List;

public class CombineRowsFn extends Combine.CombineFn<Tuple2<FileWithMeta, Try<SupplyDetail>>, StoreInventoryFullFeedAccum, Tuple2<FileWithMeta, List<Try<SupplyDetail>>>> {

    @Override
    public StoreInventoryFullFeedAccum createAccumulator() {
        return new StoreInventoryFullFeedAccum();
    }

    @Override
    public StoreInventoryFullFeedAccum addInput(StoreInventoryFullFeedAccum accumulator, Tuple2<FileWithMeta, Try<SupplyDetail>> input) {
        accumulator.add(input);
        return accumulator;
    }

    @Override
    public StoreInventoryFullFeedAccum mergeAccumulators(Iterable<StoreInventoryFullFeedAccum> accums) {
        StoreInventoryFullFeedAccum accMerged = new StoreInventoryFullFeedAccum();
        for (StoreInventoryFullFeedAccum acc : accums) {
            accMerged.extend(acc);
        }
        return accMerged;
    }

    @Override
    public Tuple2<FileWithMeta, List<Try<SupplyDetail>>> extractOutput(StoreInventoryFullFeedAccum accumulator) {
        return accumulator.get();
    }

    @Override
    public Tuple2Coder<FileWithMeta, List<Try<SupplyDetail>>> getDefaultOutputCoder(CoderRegistry registry, Coder<Tuple2<FileWithMeta, Try<SupplyDetail>>> inputCoder) {
        return Tuple2Coder.of(
            SerializableCoder.of(FileWithMeta.class), ListCoder.of(TryCoder.of(SerializableCoder.of(SupplyDetail.class))));
    }
}
