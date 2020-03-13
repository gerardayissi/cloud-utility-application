package com.tailoredbrands.util.predef;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Streams {

    public static <T> Stream<T> fromIterable(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
