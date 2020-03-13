package com.tailoredbrands.util.coder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.tailoredbrands.util.json.JsonUtils;
import org.apache.beam.sdk.coders.Coder;

import java.io.IOException;

public class Coders {

    public static Coder<JsonNode> jsonNode() {
        return Utf8Coder.of(JsonNode.class)
                .encoder(Coders::serialize)
                .decoder(Coders::deserialize)
                .build();
    }

    private static String serialize(JsonNode jsonNode) {
        try {
            return JsonUtils.mapper().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonNode deserialize(String string) {
        try {
            return JsonUtils.mapper().readTree(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
