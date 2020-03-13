package com.tailoredbrands.util.validator;

import io.vavr.collection.CharSeq;
import io.vavr.collection.List;
import io.vavr.control.Validation;

public class PubSubTopicValidator {
    private static final String VALID_NAME_CHARS = "[a-zA-Z0-9{+@.%/_}-]";
    private static final String VALID_START_NAME = "projects";

    public Validation<String, String> validateTopic(String name) {
        return Validation
                .combine(
                        validateName(name),
                        validatePrefix(name),
                        validateSeparator(name)
                )
                .ap(List::of)
                .map(topic -> topic.distinct().head())
                .mapError(err -> err.reduce((s1, s2) -> s1.concat(", " + s2)))
                .mapError(err -> throwExc(err));
    }

    private Validation<String, String> validateName(String name) {
        return CharSeq.of(name).replaceAll(VALID_NAME_CHARS, "").transform(seq ->
                seq.isEmpty() ? Validation.valid(name) : Validation.invalid("Topic name contains invalid characters: '" + seq.distinct().sorted() + "'"));
    }

    private Validation<String, String> validatePrefix(String name) {
        return CharSeq.of(name.split("/")[0]).replaceAll(VALID_START_NAME, "").transform(seq ->
                seq.isEmpty() ? Validation.valid(name) : Validation.invalid("Topic name starts with invalid prefix: '" + name.split("/")[0] + "'"));
    }

    private Validation<String, String> validateSeparator(String name) {
        return name.chars().filter(ch -> ch == '/').count() == 3 ? Validation.valid(name) :
                Validation.invalid("Topic name contains not equal to 3 '/' separators: '" + name + "'");
    }

    private String throwExc(String err) {
        throw new IllegalArgumentException(err);
    }
}