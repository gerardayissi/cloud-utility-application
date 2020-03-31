package com.tailoredbrands.util;

import com.tailoredbrands.util.control.Try;
import org.apache.beam.repackaged.core.org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.Schema;

import java.util.List;
import java.util.Optional;

/**
 * Context to parse single values raw PSV (or CSV) line.
 * Provides JSON schema for current property, value index (specified via Schema title), and required flag.
 * <p/>
 * For example: (json schema for string property 'property_name' name)
 * <pre>
 *   {
 *     "property_name" : { // property schema
 *       "type": "string",
 *       "title": "2" // value index in raw source
 *     }
 *   }
 * </pre>
 */
class Context {

    /**
     * Source to provide value for current property (and other properties from current context)
     */
    private final List<String> source;
    /**
     * Json Schema for current property. Schema title has to contain id of raw property value (from source).
     */
    private final Schema schema;
    /**
     * Value index (provided via property schema title)
     */
    private final String index;
    /**
     * Flag to indicate if property is required (not nullable).
     * Property can be nullable if it is wrapped into combined schema together with null schema.
     */
    private final boolean required;

    private Context(List<String> source, Schema schema, String index, boolean required) {
        this.source = source;
        this.schema = schema;
        this.index = index;
        this.required = required;
    }

    static Context of(List<String> source, Schema schema) {
        return new Context(source, schema, schema.getTitle(), true);
    }

    /**
     * @return optional value from specified source that is represented by schema of current property
     */
    Optional<String> value() {
        if (StringUtils.isBlank(index)) {
            return Optional.empty();
        } else {
            return Try.of(() -> Integer.parseInt(index))
                    .map(source::get)
                    .toOptional();
        }
    }

    /**
     * @return return true if property is required and not nullable.
     * In this case we might provide default value if property value is missing.
     */
    boolean isRequired() {
        return required;
    }

    /**
     * @return get values source for current properties
     */
    List<String> getSource() {
        return source;
    }

    /**
     * Get schema of current property
     *
     * @param <T> - actual type of schema, for convenience
     * @return property schema (unsafely casted into specified sub-type)
     */
    @SuppressWarnings("unchecked")
    <T extends Schema> T getSchema() {
        return (T) schema;
    }

    /**
     * @return current property marked as nullable (not required)
     */
    Context nullable() {
        return new Context(source, schema, index, false);
    }

    /**
     * Specify raw value index for current property
     *
     * @param index - raw value index
     * @return context for current property with specified index
     */
    Context withValueIndex(String index) {
        return new Context(source, schema, index, required);
    }
}
