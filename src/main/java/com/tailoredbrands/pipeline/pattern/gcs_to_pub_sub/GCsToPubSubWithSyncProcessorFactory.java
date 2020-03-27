package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.business_interface.store_inventory_full_feed.StoreInventoryFullFeedProcessor;
import com.tailoredbrands.pipeline.options.BusinessInterfaceOptions;
import com.tailoredbrands.util.FileWithMeta;
import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;

import java.util.List;

import static com.tailoredbrands.business_interface.BusinessInterface.STORE_INVENTORY_FULL_FEED;
import static io.vavr.API.Match;

public class GCsToPubSubWithSyncProcessorFactory {
    public static PTransform<PCollection<FileWithMeta>,
        PCollectionList<Tuple2<FileWithMeta, List<Try<JsonNode>>>>> from(BusinessInterfaceOptions options) {

        val businessInterface = options.getBusinessInterface();

        return Match(businessInterface).of(
            API.Case(API.$(STORE_INVENTORY_FULL_FEED.getName()), new StoreInventoryFullFeedProcessor(options))
        );
    }
}
