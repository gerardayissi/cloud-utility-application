package com.tailoredbrands.pipeline.pattern.gcs_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.business_interface.item_full_feed.ItemFullFeedProcessor;
import com.tailoredbrands.pipeline.options.BusinessInterfaceOptions;
import io.vavr.API;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.val;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;

import java.util.Map;

import static com.tailoredbrands.business_interface.BusinessInterface.ITEM_FULL_FEED;
import static io.vavr.API.Match;

public class GcsToPubSubProcessorFactory {

    public static PTransform<PCollection<Map<String, String>>,
            PCollection<Tuple2<Map<String, String>, Try<JsonNode>>>> from(BusinessInterfaceOptions options) {

        val businessInterface = options.getBusinessInterface();
        return Match(businessInterface).of(
                API.Case(API.$(ITEM_FULL_FEED.getName()), new ItemFullFeedProcessor(options))
        );
    }
}
