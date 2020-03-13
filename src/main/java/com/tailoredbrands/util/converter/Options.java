package com.tailoredbrands.util.converter;

import io.vavr.Tuple2;
import io.vavr.collection.List;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class Options {

    public static <A, B, C> Optional<C> product(Optional<A> left, Optional<B> right, BiFunction<A, B, C> fun) {
        return productK(left, right, fun.andThen(Optional::of));
    }

    public static <A> Optional<List<A>> sequence(List<Optional<A>> optionals) {
        return optionals.foldLeft(Optional.of(List.empty()), (acc, el) -> product(acc, el, List::append));
    }

    public static <A> Optional<java.util.List<A>> sequence(java.util.List<Optional<A>> optionals) {
        return sequence(List.ofAll(optionals)).map(List::asJava);
    }

    @SafeVarargs
    public static <A> Optional<java.util.List<A>> sequence(Optional<A>... optionals) {
        return sequence(Arrays.asList(optionals));
    }

    public static <A, B, C> Optional<C> productK(Optional<A> left, Optional<B> right, BiFunction<A, B, Optional<C>> fun) {
        return left.flatMap(
                leftValue -> right.flatMap(
                        rightValue -> fun.apply(leftValue, rightValue)));
    }

    public static <A, B, C> Optional<C> product(Tuple2<Optional<A>, Optional<B>> tuple, BiFunction<A, B, C> fun) {
        return product(tuple._1, tuple._2, fun);
    }

    public static <A> Optional<A> or(Optional<A> first, Optional<A> second) {
        return first.isPresent() ? first : second;
    }

    @SafeVarargs
    public static <A> Optional<A> or(Optional<A>... options) {
        return Stream.of(options)
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }
}
