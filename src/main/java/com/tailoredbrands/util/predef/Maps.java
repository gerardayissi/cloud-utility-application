package com.tailoredbrands.util.predef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Maps {

    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static <K, V> Map<K, V> merge(Map<K, V> left, Map<K, V> right) {
        if (left == null) {
            return right;
        } else if (right == null) {
            return left;
        } else {
            Map<K, V> merged = new HashMap<>();
            merged.putAll(left);
            merged.putAll(right);
            return merged;
        }
    }

    public static Map<String, String> parseMapping(String value) {
        return parseMapping(value, ",", ":");
    }

    public static Map<String, String> parseMapping(String value, String pairsDelim, String valueDelim) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyMap();
        } else {
            return Stream.of(value.split(pairsDelim))
                    .map(pair -> pair.split(valueDelim, 2))
                    .collect(Collectors.toMap(p -> p[0].trim(), p -> p[1].trim()));
        }
    }
}
