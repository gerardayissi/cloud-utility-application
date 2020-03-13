package com.tailoredbrands.util;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.CompressedSource;
import org.apache.beam.sdk.io.Compression;
import org.apache.beam.sdk.io.FileBasedSource;
import org.apache.beam.sdk.io.fs.EmptyMatchTreatment;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.util.CoderUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import static org.apache.beam.sdk.io.fs.MatchResult.Metadata;

/**
 * Returns the first text line from any file (without reading the whole file).
 * <p>
 * The input file is marked as "not splittable",
 * thus all reading from the file occurs on a single node rather than in parallel.
 *
 * <pre>
 * Usage:
 *  PCollection<String> = pipeline.apply(Read.from(HeaderSource.from(options.getInput(), EmptyMatchTreatment.DISALLOW)));
 *
 * Usually combined with .apply(View.asSingleton) to produce a PCollectionView which can then be used to pass the first line
 * of the file as a side-input to other processing steps.
 * </pre>
 */
public class HeaderSource extends FileBasedSource<String> {

    private static final int DEFAULT_MIN_BUNDLE_SIZE = 8 * 1024;

    /**
     * Factory method for use from "user code"
     */
    public static HeaderSource from(ValueProvider<String> fileOrPatternSpec, EmptyMatchTreatment emptyMatchTreatment) {
        return new HeaderSource(fileOrPatternSpec, emptyMatchTreatment);
    }

    /**
     * Factory method for use from "user code"
     */
    public static FileBasedSource<String> from(ValueProvider<String> fileOrPatternSpec,
                                               EmptyMatchTreatment emptyMatchTreatment, Compression compression) {
        HeaderSource hs = new HeaderSource(fileOrPatternSpec, emptyMatchTreatment);
        return CompressedSource.from(hs).withCompression(compression);
    }

    /**
     * Constructor used by factory method.
     */
    private HeaderSource(ValueProvider<String> fileOrPatternSpec, EmptyMatchTreatment emptyMatchTreatment) {
        super(fileOrPatternSpec, emptyMatchTreatment, DEFAULT_MIN_BUNDLE_SIZE);
    }

    /**
     * Constructor used after createForSubrangeOfFile is invoked.
     */
    private HeaderSource(Metadata metadata, long minBundleSize, long start, long end) {
        super(metadata, minBundleSize, start, end);
    }

    /**
     * No need for this source to be splittable; it only reads one line.
     */
    protected boolean isSplittable() {
        return false;
    }

    @Override
    protected FileBasedSource<String> createForSubrangeOfFile(Metadata metadata, long start, long end) {
        return new HeaderSource(metadata, getMinBundleSize(), start, end);
    }

    @Override
    protected FileBasedReader<String> createSingleFileReader(PipelineOptions options) {
        return new HeaderReader(this);
    }

    @Override
    public Coder<String> getOutputCoder() {
        return StringUtf8Coder.of();
    }

    /**
     * Object responsible for reading a specific range of the input file.
     * <p>
     * As the parent class sets isSplittable=false, there will actually be only one of these..
     * </p>
     */
    private static class HeaderReader extends FileBasedReader<String> {

        public static final String UTF8_BOM = "\uFEFF";

        private final ByteBuffer buf;
        private ReadableByteChannel channel;
        private long currOffset;
        private String currentRecord;

        HeaderReader(final HeaderSource source) {
            super(source);
            buf = ByteBuffer.allocate(4096);
            buf.flip();
        }

        @Override
        public void close() throws IOException {
            super.close();
        }

        @Override
        protected void startReading(ReadableByteChannel channel) {
            this.channel = channel;
        }

        @Override
        protected boolean readNextRecord() throws IOException {
            if (currentRecord != null) {
                // Have already read everything we need to read. Returning false here should cause the
                // close method on this class to be invoked in the near future, which will then close
                // the channel.
                return false;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            currOffset += readLine(channel, buf, out);
            currentRecord = bufToString(out);
            if (currentRecord.startsWith(UTF8_BOM)) {
                currentRecord = currentRecord.substring(1);
            }
            return true;
        }

        private static String bufToString(ByteArrayOutputStream buf) throws CoderException {
            return CoderUtils.decodeFromByteArray(StringUtf8Coder.of(), buf.toByteArray());
        }

        private static int readLine(ReadableByteChannel channel, ByteBuffer buf, ByteArrayOutputStream out) throws IOException {
            int bytesRead = 0;
            while (true) {
                if (!buf.hasRemaining()) {
                    buf.clear();
                    int read = channel.read(buf);
                    if (read < 0) {
                        break;
                    }
                    buf.flip();
                }

                byte b = buf.get();
                ++bytesRead;

                if (b == '\r') {
                    continue;
                }

                if (b == '\n') {
                    break;
                }

                out.write(b);
            }
            return bytesRead;
        }

        @Override
        protected boolean isAtSplitPoint() {
            // Every record is at a split point.
            return true;
        }

        @Override
        protected long getCurrentOffset() {
            return currOffset;
        }

        @Override
        public String getCurrent() {
            return currentRecord;
        }
    }
}