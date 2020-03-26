package com.tailoredbrands.business_interface.store_inventory_full_feed.accumulators;

import com.tailoredbrands.generated.json.store_inventory_full_feed.SupplyDetail;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.util.FileWithMeta;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.csv.CSVRecord;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.tailoredbrands.pipeline.error.ErrorType.COMBINE_OBJECTS_TO_LIST_CONVERSION_ERROR;
import static io.vavr.API.$;
import static io.vavr.API.Case;

public class StoreInventoryFullFeedAccum implements Serializable {

    private final List<String> filename = new ArrayList<>();
    private final List<String> fileContent = new ArrayList<>();
    private final List<KV<Integer, CSVRecord>> records = new ArrayList<>();

    private final List<SupplyDetail> supplyDetails = new ArrayList<>();

    public void add(Tuple2<FileWithMeta, Try<SupplyDetail>> data) {
        val fileWithMeta = data._1;
        filename.add(fileWithMeta.getSourceName());
        fileContent.add(fileWithMeta.getFileContent());
        records.addAll(fileWithMeta.getRecords());

        data.map1(row -> row)
            .map2(maybeDto -> maybeDto
                .map(supplyDetail -> supplyDetails.add(supplyDetail))
                .mapFailure(
                    Case($(e -> !(e instanceof ProcessingException)),
                        exc -> new ProcessingException(COMBINE_OBJECTS_TO_LIST_CONVERSION_ERROR, exc)))
            );
    }

    public void extend(StoreInventoryFullFeedAccum obj) {
        filename.addAll(obj.filename);
        fileContent.addAll(obj.fileContent);
        records.addAll(obj.records);
        supplyDetails.addAll(obj.supplyDetails);
    }

    public Tuple2<FileWithMeta, Try<List<SupplyDetail>>> get() {
        return new Tuple2(
            FileWithMeta.of(filename.get(0), fileContent.get(0), records),
            Try.of(() -> supplyDetails));
    }
}
