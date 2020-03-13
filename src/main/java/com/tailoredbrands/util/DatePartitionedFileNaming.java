package com.tailoredbrands.util;

import com.tailoredbrands.util.func.PartialFunction;
import org.apache.beam.sdk.io.Compression;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.transforms.windowing.IntervalWindow;
import org.apache.beam.sdk.transforms.windowing.PaneInfo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * FileNaming to place file into folder that corresponds to processing date plus default naming:
 * <pre>
 * {year}/{month}/{date}/{prefix}-{window}-{pane}-{shard}-{suffix}
 * </pre>
 */
public class DatePartitionedFileNaming implements FileIO.Write.FileNaming {

    private static final DateTimeFormatter FOLDER_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/");

    private final ValueProvider<String> prefix;
    private final ValueProvider<String> suffix;
    private final PartialFunction<BoundedWindow, LocalDate> dateFn;

    private DatePartitionedFileNaming(
            ValueProvider<String> prefix,
            ValueProvider<String> suffix,
            PartialFunction<BoundedWindow, LocalDate> dateFn
    ) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.dateFn = dateFn;
    }

    public static DatePartitionedFileNaming withWindowDate() {
        return withFn(window -> {
            if (window instanceof IntervalWindow) {
                long timestamp = ((IntervalWindow) window).start().getMillis();
                LocalDateTime instant = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
                return Optional.of(instant.toLocalDate());
            } else {
                return Optional.empty();
            }
        });
    }

    public static DatePartitionedFileNaming withDate(ValueProvider<LocalDate> date) {
        return withFn(window -> Optional.of(date.get()));
    }

    public static DatePartitionedFileNaming withDateTime(ValueProvider<LocalDateTime> date) {
        return withFn(window -> Optional.of(date.get().toLocalDate()));
    }

    public static DatePartitionedFileNaming withFn(PartialFunction<BoundedWindow, LocalDate> dateFn) {
        return new DatePartitionedFileNaming(StaticValueProvider.of(""), StaticValueProvider.of(""), dateFn);
    }

    public DatePartitionedFileNaming withPrefix(ValueProvider<String> prefix) {
        return new DatePartitionedFileNaming(prefix, suffix, dateFn);
    }

    public DatePartitionedFileNaming withSuffix(ValueProvider<String> suffix) {
        return new DatePartitionedFileNaming(prefix, suffix, dateFn);
    }

    @Override
    public String getFilename(BoundedWindow window, PaneInfo pane, int numShards, int shardIndex, Compression compression) {
        String folder = dateFn.apply(window).map(FOLDER_FORMATTER::format).orElse("");
        String filename = FileIO.Write.defaultNaming(prefix, suffix)
                .getFilename(window, pane, numShards, shardIndex, compression);

        return folder + filename;
    }
}
