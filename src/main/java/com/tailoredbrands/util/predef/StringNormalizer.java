package com.tailoredbrands.util.predef;

public class StringNormalizer {

    private static final String KEYS_REPLACEMENT_REGEXP = "[\\W]";
    private static final String FIX_IF_FIRST_CHAR_IS_NUMBER_REGEXP = "^([\\d])";

    public static String normalize(String value) {
        return value == null
                ? null
                : value.replaceAll(KEYS_REPLACEMENT_REGEXP, "_")
                .replaceAll(FIX_IF_FIRST_CHAR_IS_NUMBER_REGEXP, "_$0");
    }
}
