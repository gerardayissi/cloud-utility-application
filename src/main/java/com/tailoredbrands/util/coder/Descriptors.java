package com.tailoredbrands.util.coder;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Try;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TypeDescriptor;

/**
 * Some commonly used type descriptors.
 * <p/>
 * Useful when you need to create descriptor from non static context and as result you'd
 * capture enclosing context (object instance), which might be non-serializable.
 */
public class Descriptors {

    public static TypeDescriptor<JsonNode> jsonNode() {
        return new TypeDescriptor<JsonNode>() {};
    }

    public static TypeDescriptor<Try<JsonNode>> tryJsonNode() {
        return new TypeDescriptor<Try<JsonNode>>() {};
    }

    public static TypeDescriptor<KV<String, JsonNode>> kvStringJsonNode() {
        return new TypeDescriptor<KV<String, JsonNode>>() {};
    }

    public static TypeDescriptor<PubsubMessage> pubSubMessage() {
        return new TypeDescriptor<PubsubMessage>() {};
    }
}
