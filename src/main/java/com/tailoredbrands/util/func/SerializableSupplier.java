package com.tailoredbrands.util.func;

import java.io.Serializable;

@FunctionalInterface
public interface SerializableSupplier<R> extends Serializable {
    R get();
}
