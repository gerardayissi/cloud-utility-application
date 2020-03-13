package com.tailoredbrands.util.func;

import java.io.Serializable;
import java.util.function.BiFunction;

public interface SerializableBiFunction<T, U, R> extends BiFunction<T, U, R>, Serializable {

    @SuppressWarnings("unchecked")
    default <T1, U1, R1> SerializableBiFunction<T1, U1, R1> cast() {
        return (SerializableBiFunction<T1, U1, R1>) this;
    }

}
