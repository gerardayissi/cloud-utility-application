package com.tailoredbrands.util.schema;

import com.tailoredbrands.util.bigquery.BQDateTimeParser;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.internal.DateTimeFormatValidator;

import java.util.Optional;

/**
 * Ensure given string is valid BigQuery timestamp. It differs
 * from {@link DateTimeFormatValidator} - 'T' delimiter is optional.
 */
public class BQDateTimeFormatValidator implements FormatValidator {

    @Override
    public Optional<String> validate(String value) {
        return BQDateTimeParser.parse(value)
                .match(
                        v -> Optional.empty(),
                        e -> Optional.of(e.getMessage()));
    }

    @Override
    public String formatName() {
        return "bq-date-time";
    }
}
