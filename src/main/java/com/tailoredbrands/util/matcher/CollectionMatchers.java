package com.tailoredbrands.util.matcher;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CollectionMatchers {

    public static <T> Matcher<Collection<?>> contains(T item) {
        return facts -> facts.stream().anyMatch(item::equals);
    }

    public static <T> Matcher<Collection<?>> contains(UntypedCollectionFilter<T> filter) {
        return facts -> filter.apply(facts).count() > 0;
    }

    public static <A, B> Matcher<Collection<?>> twoElementsMatch(
            UntypedCollectionFilter<A> firstFilter,
            UntypedCollectionFilter<B> secondFilter,
            Predicate<Tuple2<A, B>> matcher) {
        return facts -> {
            Stream<A> allFirst = firstFilter.apply(facts);
            Stream<B> allSecond = secondFilter.apply(facts);
            return allFirst.anyMatch(
                    first -> allSecond.anyMatch(
                            second -> matcher.test(Tuple.of(first, second))));
        };
    }
}
