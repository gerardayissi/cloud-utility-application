package com.tailoredbrands.pipeline.pattern.jms_to_pub_sub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.business_interface.facility.FacilityProcessor;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;

import static com.tailoredbrands.business_interface.BusinessInterface.FACILITY;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

public class JmsToPubSubMultiTopicsProcessorFactory {
    public static PTransform<PCollection<Tuple2<JmsRecord, Try<String>>>, PCollectionList<Tuple2<JmsRecord, Try<JsonNode>>>> forType(String type) {
        return Match(type).of(
            Case($(FACILITY.getName()), new FacilityProcessor())
        );
    }
}
