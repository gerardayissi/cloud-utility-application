package com.tailoredbrands.business_interface.sca_do_to_fulfillment;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;

public class ScaDoToFulfillmentProcessor extends PTransform<PCollection<Tuple2<JmsRecord, Try<String>>>, PCollection<Tuple2<JmsRecord, Try<JsonNode>>>> {
    @Override
    public PCollection<Tuple2<JmsRecord, Try<JsonNode>>> expand(PCollection<Tuple2<JmsRecord, Try<String>>> input) {
        return null;
    }
}