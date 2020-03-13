package com.tailoredbrands.util.func;

import java.io.Serializable;

@FunctionalInterface
public interface SerializableConsumerE<T> extends Serializable {
    void accept(T t) throws Exception;
}
