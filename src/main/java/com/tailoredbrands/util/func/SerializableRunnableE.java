package com.tailoredbrands.util.func;

import java.io.Serializable;

@FunctionalInterface
public interface SerializableRunnableE extends Serializable {
    void run() throws Exception;
}
