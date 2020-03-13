package com.tailoredbrands.util.coder;

import com.tailoredbrands.util.func.SerializableFunction;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("squid:S2160")
public class Utf8Coder<T> extends CustomCoder<T> implements Serializable {

    public static class Builder<T> {
        private TypeDescriptor<T> type;
        private SerializableFunction<T, String> encoder;
        private SerializableFunction<String, T> decoder;

        private Builder<T> of(TypeDescriptor<T> type) {
            this.type = type;
            return this;
        }

        public Builder<T> encoder(SerializableFunction<T, String> encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder<T> decoder(SerializableFunction<String, T> decoder) {
            this.decoder = decoder;
            return this;
        }

        public Utf8Coder<T> build() {
            return new Utf8Coder<>(type, encoder, decoder);
        }
    }

    @SuppressWarnings("unused")
    private final TypeDescriptor<T> type;
    private final SerializableFunction<T, String> encoder;
    private final SerializableFunction<String, T> decoder;
    private static final NullableCoder<String> UTF8_CODER = NullableCoder.of(StringUtf8Coder.of());

    private Utf8Coder(TypeDescriptor<T> type, SerializableFunction<T, String> encoder, SerializableFunction<String, T> decoder) {
        this.type = checkNotNull(type);
        this.encoder = checkNotNull(encoder);
        this.decoder = checkNotNull(decoder);
    }

    public static <T> Builder<T> of(TypeDescriptor<T> type) {
        return new Builder<T>().of(type);
    }

    public static <T> Builder<T> of(Class<T> type) {
        return new Builder<T>().of(TypeDescriptor.of(type));
    }

    @Override
    public void encode(T value, OutputStream outStream) throws IOException {
        UTF8_CODER.encode(encoder.apply(value), outStream);
    }

    @Override
    public T decode(InputStream inStream) throws IOException {
        return decoder.apply(UTF8_CODER.decode(inStream));
    }
}
