package com.tailoredbrands.business_interface.store_inventory_full_feed.accumulators;

import com.tailoredbrands.generated.json.store_inventory_full_feed.SupplyDetail;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoreInventoryFullFeedAccum implements Serializable {

    private final List<Map<String, String>> csvRows = new ArrayList<>();
    private final List<SupplyDetail> supplyDetails = new ArrayList<>();

    public void add(Tuple2<Map<String, String>, Try<SupplyDetail>> data) {
        data
            .map1(csvRow -> csvRows.add(csvRow))
            .map2(maybeDto -> maybeDto
                .map(supplyDetail -> supplyDetails.add(supplyDetail))
            );
    }

    public void extend(StoreInventoryFullFeedAccum obj) {
        csvRows.addAll(obj.csvRows);
        supplyDetails.addAll(obj.supplyDetails);
    }

    public Tuple2<List<Map<String, String>>, Try<List<SupplyDetail>>> get() {
        return new Tuple2(csvRows, Try.of(() -> supplyDetails));
    }
}
