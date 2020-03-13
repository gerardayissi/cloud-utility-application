package com.tailoredbrands.util;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.MutablePeriod;
import org.joda.time.format.PeriodFormatterBuilder;
import org.joda.time.format.PeriodParser;

import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@link DurationUtils} class provides common utilities for manipulating and formatting {@link
 * Duration} objects.
 */
public class DurationUtils {

    public static final String USAGE = "Allowed formats are: "
            + "Ns (for seconds, example: 5s), "
            + "Nm (for minutes, example: 12m), "
            + "Nh (for hours, example: 2h). "
            + "Or their combinations, for example: 2h42m3s";

    /**
     * Parses a duration from a period formatted string. Values are accepted in the following formats:
     *
     * <p>Formats Ns - Seconds. Example: 5s<br>
     * Nm - Minutes. Example: 13m<br>
     * Nh - Hours. Example: 2h
     *
     * <pre>
     * parseDuration(null) = NullPointerException()
     * parseDuration("")   = Duration.standardSeconds(0)
     * parseDuration("2s") = Duration.standardSeconds(2)
     * parseDuration("5m") = Duration.standardMinutes(5)
     * parseDuration("3h") = Duration.standardHours(3)
     * </pre>
     *
     * @param value The period value to parse.
     * @return The {@link Duration} parsed from the supplied period string.
     */
    public static Duration parseDuration(String value) {
        checkNotNull(value, "The specified duration must be a non-null value!");

        PeriodParser parser =
                new PeriodFormatterBuilder()
                        .appendSeconds()
                        .appendSuffix("s")
                        .appendMinutes()
                        .appendSuffix("m")
                        .appendHours()
                        .appendSuffix("h")
                        .toParser();

        MutablePeriod period = new MutablePeriod();
        parser.parseInto(period, value, 0, Locale.getDefault());

        Duration duration = period.toDurationFrom(new DateTime(0));
        checkArgument(duration.getMillis() > 0, "The window duration must be greater than 0!");

        return duration;
    }

    public static java.time.Duration parseJavaDuration(String value) {
        Duration duration = parseDuration(value);
        return java.time.Duration.of(duration.getStandardSeconds(), ChronoUnit.SECONDS);
    }
}
