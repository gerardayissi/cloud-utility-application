package com.tailoredbrands.business_interface.create_order;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.generated.json.create_order.Attributes;
import com.tailoredbrands.generated.json.create_order.CreateOrderInput;
import com.tailoredbrands.generated.json.create_order.CreateOrderOutput;
import com.tailoredbrands.generated.json.create_order.Message;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.pipeline.options.JmsToPubSubOptions;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.util.Base64;

import static com.tailoredbrands.pipeline.error.ErrorType.*;
import static io.vavr.API.*;

public class CreateOrderProcessor extends PTransform<PCollection<Tuple2<JmsRecord, Try<String>>>, PCollection<Tuple2<JmsRecord, Try<JsonNode>>>> {

  @Override
  public PCollection<Tuple2<JmsRecord, Try<JsonNode>>> expand(PCollection<Tuple2<JmsRecord, Try<String>>> input) {
    return input
        .apply("Convert JSON string to POJO", convertJSONtoObject())
        .apply("Transform POJO", transform(input.getPipeline().getOptions().as(JmsToPubSubOptions.class).getTbUser()))
        .apply("Convert POJO to JSON", convertObjectToJson());
  }

  static MapElements<Tuple2<JmsRecord, Try<String>>, Tuple2<JmsRecord, Try<CreateOrderInput>>> convertJSONtoObject() {
    return MapElements
        .into(new TypeDescriptor<Tuple2<JmsRecord, Try<CreateOrderInput>>>() {
        })
        .via(tuple -> tuple.map2(
            maybeJson -> maybeJson
                .map(json -> JsonUtils.deserialize(json, CreateOrderInput.class))
                .mapFailure(
                    Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(JSON_TO_OBJECT_CONVERSION_ERROR, exc))
                )
        ));
  }

  static MapElements<Tuple2<JmsRecord, Try<CreateOrderInput>>, Tuple2<JmsRecord, Try<CreateOrderOutput>>> transform(String tbUser) {
    return MapElements
        .into(new TypeDescriptor<Tuple2<JmsRecord, Try<CreateOrderOutput>>>() {
        })
        .via(tuple -> tuple.map2(
            input ->
                input.map(inp -> {
                  Message message = new Message(
                      new Attributes(
                          tbUser,
                          inp.getOrgId()
                      ),
                      Base64.getEncoder().encodeToString(JsonUtils.serializeToBytes(inp))
                  );
                  return new CreateOrderOutput(List(message).asJava());
                })
                    .mapFailure(
                        Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_OBJECT_CONVERSION_ERROR, exc))
                    )
        ));
  }

  static MapElements<Tuple2<JmsRecord, Try<CreateOrderOutput>>, Tuple2<JmsRecord, Try<JsonNode>>> convertObjectToJson() {
    return MapElements
        .into(new TypeDescriptor<Tuple2<JmsRecord, Try<JsonNode>>>() {
        })
        .via(tuple -> tuple.map2(
            item ->
                item
                    .map(JsonUtils::toJsonNode)
                    .mapFailure(
                        Case($(e -> !(e instanceof ProcessingException)), exc -> new ProcessingException(OBJECT_TO_JSON_CONVERSION_ERROR, exc))
                    )
        ));
  }
}
