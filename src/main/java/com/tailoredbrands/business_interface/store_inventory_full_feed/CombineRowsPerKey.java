package com.tailoredbrands.business_interface.store_inventory_full_feed;

import com.tailoredbrands.generated.json.store_inventory_full_feed.SupplyDetail;
import com.tailoredbrands.util.FileWithMeta;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.ArrayList;
import java.util.List;

public class CombineRowsPerKey extends DoFn<KV<String, Iterable<Tuple2<FileWithMeta, Try<SupplyDetail>>>>, Tuple2<FileWithMeta, List<Try<SupplyDetail>>>> {

    @ProcessElement
    public void process(ProcessContext ctx) {
        List<Try<SupplyDetail>> syncPayload = new ArrayList<>();
        Iterable<Tuple2<FileWithMeta, Try<SupplyDetail>>> values = ctx.element().getValue();
        for (Tuple2<FileWithMeta, Try<SupplyDetail>> tuple2 : values) {
            syncPayload.add(tuple2._2);
        }
        FileWithMeta fileMeta = ctx.element().getValue().iterator().next()._1;
        ctx.output(new Tuple2(fileMeta, syncPayload));
    }

}
