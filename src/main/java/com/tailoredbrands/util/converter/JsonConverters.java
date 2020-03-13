package com.tailoredbrands.util.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tailoredbrands.util.control.Try;
import com.tailoredbrands.util.json.JsonUtils;
import io.vavr.Tuple2;

import java.util.Iterator;

public class JsonConverters {

    @SuppressWarnings("unchecked")
    public static Converter<ArrayNode, ArrayNode> array(Converter<? super JsonNode, ? extends JsonNode> valueConverter) {
        return Converter.of(array -> {
            // performance and memory optimized implementation
            ArrayNode converted = JsonUtils.mapper().createArrayNode();
            Iterator<JsonNode> it = array.elements();
            while (it.hasNext()) {
                Try<? extends JsonNode> result = valueConverter.apply(it.next());
                if (result.isSuccess()) {
                    converted.add(result.get());
                } else {
                    return (Try<ArrayNode>) result;
                }
            }
            return Try.success(converted);
        });
    }

    @SuppressWarnings({"unchecked", "squid:S3776"})
    public static Converter<ObjectNode, ObjectNode> object(
            Converter<String, String> keyConverter,
            Converter<? super JsonNode, ? extends JsonNode> valueConverter) {
        return Converter.of(object -> {
            // performance and memory optimized implementation
            if (object.size() == 0) {
                return Try.success(null);
            } else {
                ObjectNode converted = JsonUtils.mapper().createObjectNode();
                Iterator<String> it = object.fieldNames();
                while (it.hasNext()) {
                    String key = it.next();
                    Try<String> newKey = keyConverter.apply(key);
                    Try<? extends JsonNode> newProperty = valueConverter.apply(object.get(key));
                    Try<? extends Tuple2<String, ? extends JsonNode>> zipped = Try.zip(newKey, newProperty);
                    if (zipped.isSuccess()) {
                        JsonNode newPropertyEval = newProperty.get();
                        if (newPropertyEval != null) {
                            converted.set(newKey.get(), newPropertyEval);
                        }
                    } else {
                        return (Try<ObjectNode>) ((Object) zipped);
                    }
                }
                return Try.success(converted);
            }
        });
    }

}
