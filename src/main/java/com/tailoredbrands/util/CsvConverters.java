package com.tailoredbrands.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.tailoredbrands.util.bigquery.BQDateTimeParser;
import com.tailoredbrands.util.control.Try;
import com.tailoredbrands.util.converter.Converter;
import com.tailoredbrands.util.json.JsonUtils;
import com.tailoredbrands.util.matcher.Matcher;
import com.tailoredbrands.util.predef.Exceptions;
import com.tailoredbrands.util.schema.BQDateTimeFormatValidator;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.BooleanSchema;
import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.NullSchema;
import org.everit.json.schema.NumberSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.StringSchema;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static com.tailoredbrands.util.converter.Converters.lazy;
import static com.tailoredbrands.util.converter.Converters.match;
import static com.tailoredbrands.util.converter.Converters.when;
import static com.tailoredbrands.util.matcher.Matchers.and;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Utility to denormalize (build json object) from single line (which contains raw values) of
 * specified PSV (pipe separated values) file.
 * <p/>
 * Object structure, property types, value location, and nullability are specified by {@link ObjectSchema}.
 * Each property of given object schema can correspond to a single raw value, which will be converted
 * into specified type (of json property). If conversion has failed or value is missing - default value
 * will be provided only if property is not marked as optional. Property is treated as optional if schema
 * is combined schema ('anyOf') of actual property and null schema or type is array of actual type and 'null'.
 * <p/>
 * Title of property Schema has to contain index of raw value.
 * <p/>
 * For example:
 * <pre>
 *   PSV row:
 *   a|b|c|42|d
 *
 *   Json Schema:
 *   {
 *     "type": "object",
 *     "properties": {
 *       "name": { "type": "string", "title": "0" },
 *       "type": { "type": "string", "title": "1" },
 *       "values": {
 *         "type": "array",
 *         "items": { "type": "string", "title": "2" }
 *       },
 *       "data": {
 *         "type": "object",
 *         "properties": {
 *           "p1": { "type": "number", "title": "3" },
 *           "p2": { "type": "string", "title": "4" }
 *         }
 *       }
 *     }
 *   }
 *
 *   Result:
 *   {
 *     "name": "a",
 *     "type": "b",
 *     "values": ["c"],
 *     "data": {
 *       "p1": 42,
 *       "p2: "d"
 *     }
 *   }
 * </pre>
 */
public class CsvConverters {

    /**
     * Convert raw PVS line into JSON with given Schema.
     * Schema properties has to contain raw value indices in 'title' field.
     *
     * @param schema - json schema
     * @return PSV line into JSON converter
     */
    public static Converter<String, JsonNode> toJsonWithSchema(ObjectSchema schema, CSVFormat format) {
        return Converter
                .<String, Context>ofThrowable(values -> {
                    List<CSVRecord> source = Exceptions.exceptionally(() -> CSVParser.parse(values, format).getRecords());
                    return Context.of(Lists.newArrayList(source.get(0)), schema);
                })
                .join(convertObject());
    }

    /**
     * @return converter to parse raw values into json object by recursively converting all properties of given object
     */
    private static Converter<Context, JsonNode> convertObject() {
        return Converter.<Context, JsonNode>of(ctx -> ctx.<ObjectSchema>getSchema()
                .getPropertySchemas().entrySet().stream()
                .map(e -> Tuple.of(e.getKey(), Context.of(ctx.getSource(), e.getValue())))
                .map(t -> t.map2(convertProperty()))
                .collect(mergeResultsIntoObject()))
                .recover(ifRequired(JsonUtils.emptyObject()));
    }

    /**
     * @return converter to parse property of one of the supported types.
     * Such as: primitives, arrays, objects, nullable properties (via combined schema)
     */
    private static Converter<Context, ? extends JsonNode> convertProperty() {
        return match(
                when(and(isType(StringSchema.class), isTimestamp()), convertTimestamp()),
                when(isType(StringSchema.class), convertString()),
                when(and(isType(NumberSchema.class), isIntNumber()), convertIntNumber()),
                when(isType(NumberSchema.class), convertFloatNumber()),
                when(isType(BooleanSchema.class), convertBoolean()),
                when(isType(ObjectSchema.class), convertObject()),
                when(isType(ArraySchema.class), convertArray()),
                when(isType(CombinedSchema.class), convertNullableProperty())
        );
    }

    /**
     * Check if property has json schema of given type
     *
     * @param type - json type
     * @return type matcher
     */
    private static Matcher<Context> isType(Class<? extends Schema> type) {
        return prop -> type.isInstance(prop.getSchema());
    }

    /**
     * @return matcher to check for integer numbers
     */
    private static Matcher<Context> isIntNumber() {
        return ctx -> ctx.<NumberSchema>getSchema().requiresInteger();
    }

    /**
     * @return matcher to check for strings in timestamp format
     */
    private static Matcher<Context> isTimestamp() {
        return ctx -> ctx.<StringSchema>getSchema().getFormatValidator() instanceof BQDateTimeFormatValidator;
    }

    /**
     * @return converter to return raw string value. If property is empty - return
     * default value if property is required, or failure otherwise.
     */
    private static Converter<Context, JsonNode> convertString() {
        return convertRaw().recover(ifRequired("")).map(TextNode::valueOf);
    }

    /**
     * @return converter to try to parse timestamps into standard ISO zoned timestamp format.
     * If conversion failed (not valid timestamp) - return old value so it will go into
     * error table and can be fixed and replayed. Unless value is an empty string - in this
     * case return failed converter (empty value)
     */
    private static Converter<Context, JsonNode> convertTimestamp() {
        return convertRaw()
                .flatMap(s -> Converter.of(val -> StringUtils.isBlank(val)
                        ? Try.failure(new RuntimeException("Empty String"))
                        : BQDateTimeParser.normalize(val).recover(e -> val)))
                .map(TextNode::valueOf);
    }

    /**
     * @return converter to parse float number property. If property is empty or parsing failed - return
     * default value if property is required, or failure otherwise.
     */
    private static Converter<Context, JsonNode> convertFloatNumber() {
        return convertRaw().map(Double::parseDouble).recover(ifRequired(0D)).map(DoubleNode::valueOf);
    }

    /**
     * @return converter to parse float number property. If property is empty or parsing failed - return
     * default value if property is required, or failure otherwise.
     */
    private static Converter<Context, JsonNode> convertIntNumber() {
        return convertRaw().map(Long::parseLong).recover(ifRequired(0L)).map(LongNode::valueOf);
    }

    /**
     * @return converter to parse boolean property. If property is empty or parsing failed - return
     * default value if property is required, or failure otherwise.
     */
    private static Converter<Context, JsonNode> convertBoolean() {
        return convertRaw().map(Boolean::parseBoolean).recover(ifRequired(false)).map(BooleanNode::valueOf);
    }

    /**
     * @return converter to return property raw string value (or failure if empty)
     */
    private static Converter<Context, String> convertRaw() {
        return Converter.ofPartial(Context::value);
    }

    /**
     * Converter to build array value. If array element schema points to valid (not empty)
     * property - return array which contains single element, otherwise return failure.
     * <p/>
     * For example:
     * <pre>
     * {
     *   "names" : {
     *     "type" : "array",
     *     "items" : {
     *       "type" : "string",
     *       "title": "1" // pointer
     *     }
     *   }
     * }
     * </pre>
     *
     * @return converter to build array value. If array element schema points to valid (not empty)
     * property - return array which contains this single element, otherwise return failure.
     */
    private static Converter<Context, ArrayNode> convertArray() {
        return Converter.ofThrowable(CsvConverters::getArrayElementProperty)
                .join(lazy(CsvConverters::convertProperty))
                .map(element -> JsonUtils.emptyArray().add(element))
                .recover(ifRequired(JsonUtils.emptyArray()));
    }

    /**
     * Nullable property can be described as property with type as array with
     * two values (actual schema type and 'null') or combined schema ('anyOf') with
     * actual schema and 'null' schema.
     * Otherwise (other kind of combination) - return failure.
     * <p/>
     * For example (will return nullable string property):
     * <pre>
     *   {
     *     "name" : { "anyOf" : [ { "type" : "string", "title": 42 }, { "type" : "null" } ] }
     *   }
     *   or
     *   {
     *     "name" : { "type": ["string", "null"], "title": 42 }
     *   }
     * </pre>
     *
     * @return converter to extract optional (nullable) property
     */
    private static Converter<Context, ? extends JsonNode> convertNullableProperty() {
        return Converter.ofPartial(CsvConverters::getNullableProperty)
                .join(lazy(CsvConverters::convertProperty));
    }

    /**
     * Can be used to recover empty (or failed parsing) property by returning specified default value
     * if current properties required. Otherwise return failure.
     *
     * @param defaultValue - default value to use if property is required
     * @param <T>          - value type
     * @return recovery converter
     */
    private static <T> Converter<Context, T> ifRequired(T defaultValue) {
        return Converter.of(prop -> prop.isRequired()
                ? Try.success(defaultValue)
                : Try.failure(new RuntimeException("Missing property value")));
    }

    /**
     * Get property with schema of elements of given array property
     *
     * @param context - array property
     * @return array element property
     */
    private static Context getArrayElementProperty(Context context) {
        return Context.of(context.getSource(), context.<ArraySchema>getSchema().getAllItemSchema());
    }

    /**
     * Extract nullable property from given combined property.
     * Schema of given property has to be {@link CombinedSchema} that aggregates (via 'anyOf')
     * {@link NullSchema} and any other (that will be treated as nullable) schema.
     * Value index can be provided via child schema or parent combined schema.
     * Otherwise (if combination is invalid) - return failure.
     *
     * @param context - property with combined schema
     * @return nullable property
     */
    private static Optional<Context> getNullableProperty(Context context) {
        CombinedSchema schema = context.getSchema();
        Predicate<Schema> isNullSchema = NullSchema.class::isInstance;

        if (!schema.getCriterion().equals(CombinedSchema.ANY_CRITERION)
                || schema.getSubschemas().size() != 2
                || schema.getSubschemas().stream().noneMatch(isNullSchema)) {
            // not a nullable property
            return Optional.empty();
        } else {
            return schema.getSubschemas().stream()
                    .filter(isNullSchema.negate())
                    .map(s -> Context.of(context.getSource(), s)
                            // if nullable property is specified via multiple types - get index from parent combined schema
                            .withValueIndex(isNotBlank(s.getTitle()) ? s.getTitle() : schema.getTitle())
                            .nullable())
                    .findFirst();
        }
    }

    /**
     * Collector to collect stream of parsed properties (pair of property name and tried property) into object.
     * If result object doesn't contain any property - return failed try.
     *
     * @param <T> - type of properties
     * @return collected object
     */
    private static <T extends JsonNode> Collector<Tuple2<String, Try<T>>, ObjectNode, Try<JsonNode>> mergeResultsIntoObject() {
        return Collector.of(
                JsonUtils::emptyObject,
                (o, t) -> t._2.doOnSuccess(val -> o.set(t._1, val)),
                (o1, o2) -> (ObjectNode) o1.setAll(o2),
                o -> o.size() == 0 ? Try.failure(new RuntimeException("Empty Object")) : Try.success(o)
        );
    }
}
