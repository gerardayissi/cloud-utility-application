package com.tailoredbrands.util.bigquery;

import com.google.cloud.Timestamp;
import com.tailoredbrands.util.control.Try;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Locale;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

public class BQDateTimeParser {

    // NOTE: BQ time format is slightly different from the default ISO time format:
    // - seconds are required here
    private static final DateTimeFormatter BQ_LOCAL_TIME =
            new DateTimeFormatterBuilder()
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(':')
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .optionalStart()
                    .appendFraction(NANO_OF_SECOND, 1, 9, true)
                    .toFormatter(Locale.getDefault(Locale.Category.FORMAT));

    private static final DateTimeFormatter BQ_ZONED_DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .append(new DateTimeFormatterBuilder()
                            .parseCaseInsensitive()
                            .append(new DateTimeFormatterBuilder()
                                    .parseCaseInsensitive()
                                    .append(new DateTimeFormatterBuilder()
                                            .appendValue(YEAR, 4, 10, SignStyle.NOT_NEGATIVE)
                                            .appendLiteral('-')
                                            .appendValue(MONTH_OF_YEAR, 2)
                                            .appendLiteral('-')
                                            .appendValue(DAY_OF_MONTH, 2)
                                            .toFormatter())
                                    .appendLiteral('T')
                                    .append(BQ_LOCAL_TIME)
                                    .toFormatter(Locale.getDefault(Locale.Category.FORMAT)))
                            .appendOffsetId()
                            .toFormatter(Locale.getDefault(Locale.Category.FORMAT)))
                    .optionalStart()
                    .appendLiteral('[')
                    .parseCaseSensitive()
                    .appendZoneRegionId()
                    .appendLiteral(']')
                    .toFormatter(Locale.getDefault(Locale.Category.FORMAT));

    private static final DateTimeFormatter BQ_LOCAL_DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral('T')
                    .append(BQ_LOCAL_TIME)
                    .toFormatter(Locale.getDefault(Locale.Category.FORMAT));

    /**
     * Parse timestamp in BigQuery format (which is a superset of ISO datetime)
     *
     * @param timestamp timestamp in format <code>YYYY-[M]M-[D]D[( |T)[H]H:[M]M:[S]S[.DDDDDD]][time zone]</code>,
     *                  when zone is missing we assume it is UTC
     * @return standard Java {@link ZonedDateTime} in UTC timezone
     */
    public static Try<ZonedDateTime> parse(String timestamp) {
        String timestampWithT = timestamp.replace(" ", "T");
        return parseZonedDatetime(timestampWithT)
                .orElse(parseLocalDatetime(timestampWithT));
    }

    /**
     * Try to parse given timestamp and convert it into ISO zoned datetime format
     *
     * @param timestamp - timestamp
     * @return ISO formatted string or failure
     */
    public static Try<String> normalize(String timestamp) {
        return parse(timestamp).map(DateTimeFormatter.ISO_ZONED_DATE_TIME::format);
    }

    private static Try<ZonedDateTime> parseZonedDatetime(String datetime) {
        return Try
                .of(() -> BQ_ZONED_DATE_TIME_FORMATTER.parse(datetime, ZonedDateTime::from))
                .map(t -> t.withZoneSameInstant(ZoneOffset.UTC));
    }

    private static Try<ZonedDateTime> parseLocalDatetime(String datetime) {
        return Try
                .of(() -> BQ_LOCAL_DATE_TIME_FORMATTER.parse(datetime, LocalDateTime::from))
                .map(local -> local.atZone(ZoneOffset.UTC));
    }

    /**
     * Converts standard Java {@link ZonedDateTime} into Google {@link Timestamp}
     *
     * @param zonedDateTime datetime
     * @return timestamp
     */
    public static Timestamp toTimestamp(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            return Timestamp.ofTimeMicroseconds(zonedDateTime.toInstant().toEpochMilli() * 1000);
        } else {
            return null;
        }
    }
}