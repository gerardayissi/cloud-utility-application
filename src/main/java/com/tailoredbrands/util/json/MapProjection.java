package com.tailoredbrands.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.io.IOException;
import java.io.InputStream;

public class MapProjection implements Projection {

    private static final String DEFAULT_FIELD = "*";
    private final Map<String, Boolean> schema;

    public MapProjection(Map<String, Boolean> schema) {
        this.schema = schema;
    }

    public static Projection of(Map<String, Boolean> schema) {
        if (schema.size() == 1 && schema.containsKey(DEFAULT_FIELD)) {
            // use static projections for schemas with only default field
            return schema.getOrElse(DEFAULT_FIELD, false) ? Projection.all() : Projection.none();
        } else {
            return new MapProjection(schema);
        }
    }

    public static Projection deserialize(InputStream json) {
        try {
            Map<String, Boolean> schema = JsonUtils.mapper()
                    .readValue(json, new TypeReference<HashMap<String, Boolean>>() {});
            return MapProjection.of(schema);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Projection> deserializeMultiple(InputStream json) {
        try {
            Map<String, Map<String, Boolean>> schemas = JsonUtils.mapper()
                    .readValue(json, new TypeReference<HashMap<String, HashMap<String, Boolean>>>() {});
            return HashMap.ofEntries(schemas
                    .filter((type, schema) -> schema.containsValue(true))
                    .map(t -> Tuple.of(t._1(), MapProjection.of(t._2()))));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean acceptField(String name) {
        return schema.get(name).getOrElse(() -> schema.getOrElse(DEFAULT_FIELD, false));
    }

    @Override
    public String toString() {
        return schema.toString();
    }
}
