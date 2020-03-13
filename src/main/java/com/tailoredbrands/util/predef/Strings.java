package com.tailoredbrands.util.predef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {

    public static List<String> split(String value, String regex, int limit) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(value.split(regex, limit));
        }
    }

    public static List<String> split(String value, String regex) {
        return split(value, regex, 0);
    }

    public static List<String> extractFromRegex(String value, String regex) {
        List<String> values = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    public static Optional<String> extractFromRegexFirst(String value, String regex) {
        List<String> values = extractFromRegex(value, regex);
        return values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean nonBlank(String value) {
        return !isBlank(value);
    }
}
