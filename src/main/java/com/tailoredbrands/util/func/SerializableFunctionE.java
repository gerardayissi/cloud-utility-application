package com.tailoredbrands.util.func;

import java.io.Serializable;

@FunctionalInterface
public interface SerializableFunctionE<T, R> extends Serializable {
    R apply(T t) throws Exception;
}
