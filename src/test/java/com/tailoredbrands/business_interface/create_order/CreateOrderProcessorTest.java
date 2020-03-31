package com.tailoredbrands.business_interface.create_order;

import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.generated.json.create_order.Attributes;
import com.tailoredbrands.generated.json.create_order.CreateOrderInput;
import com.tailoredbrands.generated.json.create_order.CreateOrderOutput;
import com.tailoredbrands.generated.json.create_order.Message;
import com.tailoredbrands.pipeline.error.ErrorType;
import com.tailoredbrands.pipeline.error.ProcessingException;
import com.tailoredbrands.util.coder.TryCoder;
import com.tailoredbrands.util.coder.Tuple2Coder;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.predef.Resources;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;

import static com.tailoredbrands.testutil.Matchers.hasItems;
import static com.tailoredbrands.testutil.Matchers.map;
import static com.tailoredbrands.testutil.TestUtils.jmsRecord;
import static io.vavr.API.List;
import static org.hamcrest.CoreMatchers.containsString;

public class CreateOrderProcessorTest implements Serializable {
  @Rule
  public final transient TestPipeline pipeline = TestPipeline.create();

  @Test
  public void convertJsonStringToObject() {
    PCollection<Tuple2<JmsRecord, Try<CreateOrderInput>>> pc = pipeline
        .apply(Create.of(
            new Tuple2<>(jmsRecord(Resources.readAsString("create_order/CreateOrderInput.json")), Try.success(Resources.readAsString("create_order/CreateOrderInput.json")))
        ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
        .apply(CreateOrderProcessor.convertJSONtoObject());
    PAssert.that(pc).satisfies(it -> hasItems(it,
        map(t -> t._2.get().getOrgId(), containsString("TMW")
        )
    ));
    pipeline.run();
  }

  @Test
  public void convertJsonStringToObjectFailure() {
    PCollection<Tuple2<JmsRecord, Try<CreateOrderInput>>> pc = pipeline
        .apply(Create.of(
            new Tuple2<>(jmsRecord(Resources.readAsString("create_order/CreateOrderInput.json")), Try.success("DEFINITELY_NOT_JSON"))
        ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
        .apply(CreateOrderProcessor.convertJSONtoObject());
    PAssert.that(pc).satisfies(it -> hasItems(it,
        map(t -> ((ProcessingException) t._2.failed().get()).getType(), CoreMatchers.is(ErrorType.JSON_TO_OBJECT_CONVERSION_ERROR))
    ));
    pipeline.run();
  }

  @Test
  public void convertOutputToJson() {
    CreateOrderOutput jsonItem = new CreateOrderOutput();
    jsonItem.setMessages(List(new Message(new Attributes("admin@tb.com", "TMW"), "TEST_DATA")).asJava());
    PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
        .apply(Create.of(
            new Tuple2<>(jmsRecord(Resources.readAsString("create_order/CreateOrderInput.json")), Try.success(jsonItem))
        ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(SerializableCoder.of(CreateOrderOutput.class)))))
        .apply(CreateOrderProcessor.convertObjectToJson());
    PAssert.that(pc).satisfies(it -> hasItems(it,
        map(t -> t._2.get().get("messages").get(0).get("data").asText(), containsString("TEST_DATA"))
    ));
    pipeline.run();
  }

  @Test
  public void objectTransformation() {
    CreateOrderInput inputJson = JsonUtils.deserialize(Resources.readAsString("create_order/CreateOrderInput.json"), CreateOrderInput.class);
    PCollection<Tuple2<JmsRecord, Try<CreateOrderOutput>>> pc = pipeline
        .apply(Create.of(
            new Tuple2<>(jmsRecord(Resources.readAsString("create_order/CreateOrderInput.json")), Try.success(inputJson))
        ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(SerializableCoder.of(CreateOrderInput.class)))))
        .apply(CreateOrderProcessor.transform("admin@tbi.com"));
    PAssert.that(pc).satisfies(it -> hasItems(it,
        map(t -> t._2.get().getMessages().get(0).getAttributes().getOrganization(), containsString(inputJson.getOrgId())
        )
    ));
    pipeline.run();
  }

  @Test
  public void endToEndProcessorTest() {
    PCollection<Tuple2<JmsRecord, Try<JsonNode>>> pc = pipeline
        .apply(Create.of(
            new Tuple2<>(jmsRecord(Resources.readAsString("create_order/CreateOrderInput.json")), Try.success(Resources.readAsString("create_order/CreateOrderInput.json")))
        ).withCoder(Tuple2Coder.of(SerializableCoder.of(JmsRecord.class), TryCoder.of(StringUtf8Coder.of()))))
        .apply("JSON to object", CreateOrderProcessor.convertJSONtoObject())
        .apply("Transform", CreateOrderProcessor.transform("admin@tbi.com"))
        .apply("Object to JSON", CreateOrderProcessor.convertObjectToJson());
    PAssert.that(pc).satisfies(it -> hasItems(it,
        map(t -> t._2.get().get("messages").get(0).get("attributes").get("Organization").asText(), containsString("TMW"))
    ));
    pipeline.run();
  }
}
