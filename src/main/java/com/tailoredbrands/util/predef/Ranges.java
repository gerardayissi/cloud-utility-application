package com.tailoredbrands.util.predef;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class Ranges {

    public static <T extends Comparable<T>, R extends Comparable<R>> Range<R> map(Range<T> range, Function<T, R> fn) {
        Optional<Range<R>> mapped = fold(
                range,
                (l, b) -> Range.downTo(fn.apply(l), b),
                (u, b) -> Range.upTo(fn.apply(u), b),
                Range::intersection
        );
        return mapped.orElse(Range.all());
    }

    public static <T extends Comparable<T>, R> Optional<R> fold(
            Range<T> range,
            BiFunction<T, BoundType, R> lowerFn,
            BiFunction<T, BoundType, R> upperFn,
            BinaryOperator<R> combiner
    ) {
        if (range.hasLowerBound() && range.hasUpperBound()) {
            return Optional.of(combiner.apply(
                    lowerFn.apply(range.lowerEndpoint(), range.lowerBoundType()),
                    upperFn.apply(range.upperEndpoint(), range.upperBoundType())
            ));
        } else if (range.hasLowerBound()) {
            return Optional.of(lowerFn.apply(range.lowerEndpoint(), range.lowerBoundType()));
        } else if (range.hasUpperBound()) {
            return Optional.of(upperFn.apply(range.upperEndpoint(), range.upperBoundType()));
        } else {
            return Optional.empty();
        }
    }
}
