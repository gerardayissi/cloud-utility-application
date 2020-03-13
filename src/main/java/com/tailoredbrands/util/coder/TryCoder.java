package com.tailoredbrands.util.coder;

import io.vavr.control.Try;
import org.apache.beam.sdk.coders.BooleanCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.StructuredCoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("squid:S2160")
public class TryCoder<A> extends StructuredCoder<Try<A>> {

    private static final BooleanCoder BOOLEAN_CODER = BooleanCoder.of();
    private static final ThrowableCoder EXCEPTION_CODER = ThrowableCoder.of();
    private final Coder<A> resultCoder;

    private TryCoder(Coder<A> resultCoder) {
        this.resultCoder = resultCoder;
    }

    public static <B> TryCoder<B> of(Coder<B> resultCoder) {
        return new TryCoder<>(checkNotNull(resultCoder, "'resultCoder' is required"));
    }

    @Override
    public void encode(Try<A> value, OutputStream outStream) throws IOException {
        checkNotNull(value, "The TryCoder cannot encode a null object, wrap it into NullableCoder");
        if (value.isSuccess()) {
            BOOLEAN_CODER.encode(true, outStream);
            resultCoder.encode(value.get(), outStream);
        } else {
            BOOLEAN_CODER.encode(false, outStream);
            EXCEPTION_CODER.encode(value.failed().get(), outStream);
        }
    }

    @Override
    public Try<A> decode(InputStream inStream) throws IOException {
        Boolean success = BOOLEAN_CODER.decode(inStream);
        if (success) {
            return Try.success(resultCoder.decode(inStream));
        } else {
            return Try.failure(EXCEPTION_CODER.decode(inStream));
        }
    }

    @Override
    public List<? extends Coder<?>> getCoderArguments() {
        return Collections.singletonList(resultCoder);
    }

    @Override
    public void verifyDeterministic() throws NonDeterministicException {
        resultCoder.verifyDeterministic();
    }
}
