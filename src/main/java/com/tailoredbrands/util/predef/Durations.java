package com.tailoredbrands.util.predef;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.MutablePeriod;
import org.joda.time.format.PeriodFormatterBuilder;
import org.joda.time.format.PeriodParser;

import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Durations {

    public static final String USAGE = "Allowed formats are: "
            + "Ns (for seconds, example: 5s), "
            + "Nm (for minutes, example: 12m), "
            + "Nh (for hours, example: 2h). "
            + "Or their combinations, for example: 2h42m3s";

    private static Duration parseJoda(String value) {
        checkNotNull(value, "The specified duration must be a non-null value!");
        PeriodParser parser =
                new PeriodFormatterBuilder()
                        .appendSeconds()
                        .appendSuffix("s")
                        .appendMinutes()
                        .appendSuffix("m")
                        .appendHours()
                        .appendSuffix("h")
                        .appendDays()
                        .appendSuffix("d")
                        .toParser();
        MutablePeriod period = new MutablePeriod();
        parser.parseInto(period, value, 0, Locale.getDefault());
        Duration duration = period.toDurationFrom(new DateTime(0));
        checkArgument(duration.getMillis() > 0, "The duration must be greater than 0");
        return duration;
    }

    /**
     * Parses a duration from a period formatted string. Values are accepted in the following formats:
     *
     * <p>Formats Ns - Seconds. Example: 5s<br>
     * Nm - Minutes. Example: 13m<br>
     * Nh - Hours. Example: 2h
     *
     * <pre>
     * parseDuration(null) = NullPointerException()
     * parseDuration("")   = Duration.ofSeconds(0)
     * parseDuration("2s") = Duration.ofSeconds(2)
     * parseDuration("5m") = Duration.ofMinutes(5)
     * parseDuration("3h") = Duration.ofHours(3)
     * parseDuration("7d") = Duration.ofDays(7)
     * </pre>
     *
     * @param value The period value to parse.
     * @return The {@link Duration} parsed from the supplied period string.
     */
    public static java.time.Duration parse(String value) {
        Duration duration = parseJoda(value);
        return java.time.Duration.of(duration.getStandardSeconds(), ChronoUnit.SECONDS);
    }

}
