package com.tailoredbrands.util.func;

import java.io.Serializable;

@FunctionalInterface
public interface SerializableSupplierE<R> extends Serializable {
    R get() throws Exception;
}
