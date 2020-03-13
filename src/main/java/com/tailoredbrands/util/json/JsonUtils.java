package com.tailoredbrands.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tailoredbrands.util.control.Try;
import com.tailoredbrands.util.converter.Options;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.jackson.datatype.VavrModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Friendly API to work with low level JSON node API
 */
public class JsonUtils {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .registerModule(new VavrModule())
                    .registerModule(new JavaTimeModule())
                    .disable(WRITE_DATES_AS_TIMESTAMPS); // write local dates as string instead of array

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        return MAPPER.convertValue(object, Map.class);
    }

    public static String serializeToString(JsonNode jsonNode) {
        try {
            return mapper().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] serializeToBytes(JsonNode jsonNode) {
        try {
            return mapper().writeValueAsBytes(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode deserialize(String string) {
        try {
            return mapper().readTree(string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serializeObject(Object value) {
        try {
            return mapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode toJsonNode(Object value) {
        return mapper().convertValue(value, JsonNode.class);
    }

    public static byte[] serializeToBytes(Object value) {
        try {
            return mapper().writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode deserialize(byte[] bytes) {
        try {
            return MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode deserialize(InputStream is) {
        try {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<JsonNode> deserializeOptional(String string) {
        try {
            return Optional.of(deserialize(string));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static <T> T deserialize(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<ObjectNode> getObjectPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && property.isObject()) {
            return Optional.of((ObjectNode) property);
        } else {
            return Optional.empty();
        }
    }

    public static ObjectNode getObjectProperty(JsonNode node, String propertyName) {
        return getObjectPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    private static Supplier<IllegalArgumentException> requiredPropertyError(String propertyName) {
        return () -> new IllegalArgumentException("json property '" + propertyName + "' is required");
    }

    public static Optional<ArrayNode> getArrayPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && property.isArray()) {
            return Optional.of((ArrayNode) property);
        } else {
            return Optional.empty();
        }
    }

    public static ArrayNode getArrayProperty(JsonNode node, String propertyName) {
        return getArrayPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Boolean> getBooleanPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && property.isBoolean()) {
            return Optional.of(property.booleanValue());
        } else {
            return Optional.empty();
        }
    }

    public static boolean getBooleanProperty(JsonNode node, String propertyName) {
        return getBooleanPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<String> getStringPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && property.isTextual()) {
            return Optional.of(property.textValue());
        } else {
            return Optional.empty();
        }
    }

    public static String getStringProperty(JsonNode node, String propertyName) {
        return getStringPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<String> getNotEmptyStringPropertyOptional(JsonNode node, String propertyName) {
        return getStringPropertyOptional(node, propertyName).filter(s -> !s.trim().isEmpty());
    }

    public static String getNotEmptyStringProperty(JsonNode node, String propertyName) {
        return getNotEmptyStringPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<String> asStringPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null) {
            return Optional.of(property.asText());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Long> asLongPropertyOptional(JsonNode node, String propertyName) {
        return Options.or(
                getLongPropertyOptional(node, propertyName),
                getStringPropertyOptional(node, propertyName)
                        .flatMap(l -> Try.of(() -> Long.parseLong(l)).toOptional())
        );
    }

    public static String asStringProperty(JsonNode node, String propertyName) {
        return asStringPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<String> getPropertyAsTextOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null) {
            return Optional.of(property.toString());
        } else {
            return Optional.empty();
        }
    }

    public static String getPropertyAsText(JsonNode node, String propertyName) {
        return getPropertyAsTextOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<ZonedDateTime> getTimestampPropertyOptional(JsonNode node, String propertyName) {
        return getStringPropertyOptional(node, propertyName)
                .map(ZonedDateTime::parse);
    }

    public static ZonedDateTime getTimestampProperty(JsonNode node, String propertyName) {
        return getTimestampPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<ZonedDateTime> getEpochTimePropertyOptional(JsonNode node, String propertyName) {
        return getLongPropertyOptional(node, propertyName)
                .map(ms -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC));
    }

    public static ZonedDateTime getEpochTimeProperty(JsonNode node, String propertyName) {
        return getEpochTimePropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<LocalDateTime> getLocalTimestampPropertyOptional(JsonNode node, String propertyName) {
        return getStringPropertyOptional(node, propertyName)
                .map(LocalDateTime::parse);
    }

    public static LocalDateTime getLocalTimestampProperty(JsonNode node, String propertyName) {
        return getLocalTimestampPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Number> getNumberPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.numberValue());
        } else {
            return Optional.empty();
        }
    }

    public static Number getNumberProperty(JsonNode node, String propertyName) {
        return getNumberPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Double> getDoublePropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.doubleValue());
        } else {
            return Optional.empty();
        }
    }

    public static double getDoubleProperty(JsonNode node, String propertyName) {
        return getDoublePropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Float> getFloatPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.floatValue());
        } else {
            return Optional.empty();
        }
    }

    public static float getFloatProperty(JsonNode node, String propertyName) {
        return getFloatPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Long> getLongPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.longValue());
        } else {
            return Optional.empty();
        }
    }

    public static long getLongProperty(JsonNode node, String propertyName) {
        return getLongPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Integer> getIntPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.intValue());
        } else {
            return Optional.empty();
        }
    }

    public static int getIntProperty(JsonNode node, String propertyName) {
        return getIntPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<Short> getShortPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.shortValue());
        } else {
            return Optional.empty();
        }
    }

    public static short getShortProperty(JsonNode node, String propertyName) {
        return getShortPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<BigInteger> getBigIntegerPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.bigIntegerValue());
        } else {
            return Optional.empty();
        }
    }

    public static BigInteger getBigIntegerProperty(JsonNode node, String propertyName) {
        return getBigIntegerPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<BigDecimal> getBigDecimalPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && (property.isIntegralNumber() || property.isFloatingPointNumber())) {
            return Optional.of(property.decimalValue());
        } else {
            return Optional.empty();
        }
    }

    public static BigDecimal getBigDecimalProperty(JsonNode node, String propertyName) {
        return getBigDecimalPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static Optional<byte[]> getBinaryPropertyOptional(JsonNode node, String propertyName) {
        JsonNode property = node.get(propertyName);
        if (property != null && property.isBinary()) {
            try {
                return Optional.of(property.binaryValue());
            } catch (IOException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static byte[] getBinaryProperty(JsonNode node, String propertyName) {
        return getBinaryPropertyOptional(node, propertyName).orElseThrow(requiredPropertyError(propertyName));
    }

    public static List<JsonNode> childrenOf(ArrayNode node) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(node.elements(), Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    public static ArrayNode asArrayNode(List<JsonNode> elements) {
        return new ArrayNode(JsonNodeFactory.instance, elements);
    }

    public static Optional<JsonNode> lastChildrenOf(ArrayNode node) {
        List<JsonNode> all = childrenOf(node);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1));
    }

    public static List<Tuple2<String, JsonNode>> propertiesOf(ObjectNode node) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(node.fieldNames(), Spliterator.ORDERED), false)
                .map(name -> Tuple.of(name, node.get(name)))
                .collect(Collectors.toList());
    }

    public static List<String> propertyNamesOf(JsonNode node) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(node.fieldNames(), Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    public static ObjectNode merge(ObjectNode left, ObjectNode right) {
        return mergeIntoLeft(left.deepCopy(), right);
    }

    public static ObjectNode mergeIntoLeft(ObjectNode left, ObjectNode right) {
        JsonUtils.propertiesOf(right).forEach(tuple -> {
            JsonNode existing = left.get(tuple._1);
            if (existing == null || existing.isNull()) {
                left.set(tuple._1, tuple._2);
            }
        });
        return left;
    }

    public static JsonNode extractFromPath(JsonNode node, String path) {
        String[] steps = path.split("\\.");
        String property = steps[0];
        if (steps.length == 1) {
            return node.findPath(property);
        }
        JsonNode subNode = node.findPath(property);
        if (subNode.isMissingNode()) {
            return subNode;
        }
        return extractFromPath(extractFromEncodedString(subNode),
                IntStream.range(1, steps.length)
                        .mapToObj(idx -> steps[idx])
                        .collect(Collectors.joining(".")));
    }

    public static JsonNode extractFromEncodedString(JsonNode nodeText) {
        if (nodeText.isTextual()) {
            return JsonUtils.deserialize(nodeText.asText());
        }
        return nodeText;
    }

    /**
     * Get required field of the given node and cast it into inferred JsonNode subtype
     * (convenient if Object, Array, or Number Nodes are needed)
     *
     * @param node      - parent node
     * @param fieldName - field name
     * @param <T>       - type of the field, if not specified - JsonNode is inferred
     * @return field value or throw
     * @throws RuntimeException if field is empty
     */
    @SuppressWarnings("unchecked")
    public static <T extends JsonNode> T getRequired(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);

        checkNotNull(field, "Required field '%s' is missing in '%s'", fieldName, node);
        checkArgument(!field.isNull(), "Required field '%s' is null in '%s'", fieldName, node);

        return (T) field;
    }

    /**
     * Create shallow copy of the given json Object Node.
     * Container (object, array) children nodes will still share references -
     * which means modifying top level children is safe, modifying deeper
     * nested fields is not.
     *
     * @param object - json object
     * @return shallow copy
     */
    public static ObjectNode shallowCopy(ObjectNode object) {
        ObjectNode copy = mapper().createObjectNode();
        copy.setAll(object);
        return copy;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static ArrayNode emptyArray() {
        return mapper().createArrayNode();
    }

    public static ObjectNode emptyObject() {
        return mapper().createObjectNode();
    }

    public static ObjectNode emptySerializableObject() {
        return new SerializableObjectNode();
    }

    public static <T extends JsonNode> Collector<Tuple2<String, T>, ObjectNode, ObjectNode> collectObjectNode() {
        return Collector.of(
                JsonUtils::emptyObject,
                (o, t) -> o.set(t._1, t._2),
                (o1, o2) -> (ObjectNode) o1.setAll(o2));
    }

    public static <T extends JsonNode> Collector<T, ArrayNode, ArrayNode> collectArrayNode() {
        return Collector.of(JsonUtils::emptyArray, ArrayNode::add, ArrayNode::addAll);
    }

    private static class SerializableObjectNodeAdapter extends ObjectNode {

        SerializableObjectNodeAdapter() {
            super(JsonNodeFactory.instance);
        }
    }

    // ObjectNode is not serializable, so to have this adapter we still need first non-serializable
    // super type with no-arg constructor - SerializableObjectNodeAdapter
    private static class SerializableObjectNode extends SerializableObjectNodeAdapter implements Serializable {

    }
}
