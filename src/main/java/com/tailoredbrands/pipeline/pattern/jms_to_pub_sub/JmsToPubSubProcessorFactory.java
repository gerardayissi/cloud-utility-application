package com.tailoredbrands.pipeline.pattern.jms_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.business_interface.create_order.CreateOrderProcessor;
import com.tailoredbrands.business_interface.item_delta_feed.ItemDeltaFeedProcessor;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;

import static com.tailoredbrands.business_interface.BusinessInterface.CREATE_ORDER;
import static com.tailoredbrands.business_interface.BusinessInterface.ITEM_DELTA_FEED;
import static io.vavr.API.*;

public class JmsToPubSubProcessorFactory {
  public static PTransform<PCollection<Tuple2<JmsRecord, Try<String>>>, PCollection<Tuple2<JmsRecord, Try<JsonNode>>>> forType(String type) {
    return Match(type).of(
        Case($(ITEM_DELTA_FEED.getName()), new ItemDeltaFeedProcessor()),
        Case($(CREATE_ORDER.getName()), new CreateOrderProcessor())
    );
  }
}
