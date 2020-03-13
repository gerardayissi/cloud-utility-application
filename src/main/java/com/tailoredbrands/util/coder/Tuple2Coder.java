package com.tailoredbrands.util.coder;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.StructuredCoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("squid:S2160")
public class Tuple2Coder<K, V> extends StructuredCoder<Tuple2<K, V>> {

    private final Coder<K> firstCoder;
    private final Coder<V> secondCoder;

    private Tuple2Coder(Coder<K> firstCoder, Coder<V> secondCoder) {
        this.firstCoder = NullableCoder.of(firstCoder);
        this.secondCoder = NullableCoder.of(secondCoder);
    }

    public static <K, V> Tuple2Coder<K, V> of(Coder<K> firstCoder, Coder<V> secondCoder) {
        return new Tuple2Coder<>(firstCoder, secondCoder);
    }

    public Coder<K> getFirstCoder() {
        return firstCoder;
    }

    public Coder<V> getSecondCoder() {
        return secondCoder;
    }

    @Override
    public void encode(Tuple2<K, V> value, OutputStream outStream) throws IOException {
        checkNotNull(value, "The Tuple2Coder cannot encode a null object, wrap it into NullableCoder");
        firstCoder.encode(value._1, outStream);
        secondCoder.encode(value._2, outStream);
    }

    @Override
    public Tuple2<K, V> decode(InputStream inStream) throws IOException {
        return Tuple.of(firstCoder.decode(inStream), secondCoder.decode(inStream));
    }

    @Override
    public List<? extends Coder<?>> getCoderArguments() {
        return Arrays.asList(firstCoder, secondCoder);
    }

    @Override
    public void verifyDeterministic() throws NonDeterministicException {
        firstCoder.verifyDeterministic();
        secondCoder.verifyDeterministic();
    }

}
