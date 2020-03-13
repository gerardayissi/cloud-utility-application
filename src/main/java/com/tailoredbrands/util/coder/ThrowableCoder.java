package com.tailoredbrands.util.coder;

import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("squid:S2160")
public class ThrowableCoder extends CustomCoder<Throwable> {

    private ThrowableCoder() {}

    public static ThrowableCoder of() {
        return new ThrowableCoder();
    }

    @Override
    public void encode(Throwable value, OutputStream outStream) throws IOException {
        checkNotNull(value, "The ExceptionCoder cannot encode a null object, wrap it into NullableCoder");
        try {
            new ObjectOutputStream(outStream).writeObject(value);
        } catch (Exception e) {
            throw new CoderException(e);
        }
    }

    @Override
    public Exception decode(InputStream inStream) throws IOException {
        try {
            return (Exception) new ObjectInputStream(inStream).readObject();
        } catch (ClassNotFoundException e) {
            throw new CoderException(e);
        }
    }
}
