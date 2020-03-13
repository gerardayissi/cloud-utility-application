package com.tailoredbrands.util.matcher;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Matchers {

    public static <T> Matcher<T> just(boolean result) {
        return facts -> result;
    }

    public static <T> Matcher<T> and(Collection<Matcher<T>> matchers) {
        return value -> matchers.stream().allMatch(matcher -> matcher.test(value));
    }

    public static <T> Matcher<T> and(Matcher<T> first, Matcher<T> second) {
        return value -> first.test(value) && second.test(value);
    }

    public static <T> Matcher<T> ar(Collection<Matcher<T>> matchers) {
        return value -> matchers.stream().anyMatch(matcher -> matcher.test(value));
    }

    @SuppressWarnings("unchecked")
    public static <A extends C, B extends C, C> Matcher<C> or(Matcher<A> first, Matcher<B> second) {
        return value -> first.test((A) value) || second.test((B) value);
    }

    @SuppressWarnings("unchecked")
    public static <A extends D, B extends D, C extends D, D> Matcher<D> or(Matcher<A> first, Matcher<B> second, Matcher<C> third) {
        return value -> first.test((A) value) || second.test((B) value) || third.test((C) value);
    }

    @SafeVarargs
    public static <A> Matcher<A> or(Matcher<A>... matchers) {
        return value -> Arrays.stream(matchers).anyMatch(matcher -> matcher.test(value));
    }

    public static <T> Matcher<T> not(Matcher<T> matcher) {
        return value -> !matcher.test(value);
    }

    public static <T> Matcher<T> any() {
        return value -> true;
    }

    public static <T> Matcher<T> none() {
        return value -> false;
    }

    public static <T> Matcher<T> isInstanceOf(Class<?> clazz) {
        return clazz::isInstance;
    }

    public static <T> Matcher<?> isInstance(Class<T> clazz) {
        return clazz::isInstance;
    }

    public static <T> Matcher<T> isString() {
        return isInstanceOf(String.class);
    }

    public static <T> Matcher<T> isInteger() {
        return isInstanceOf(Integer.class);
    }

    public static <T> Matcher<T> isLong() {
        return isInstanceOf(Long.class);
    }

    public static <T> Matcher<T> isShort() {
        return isInstanceOf(Short.class);
    }

    public static <T> Matcher<T> isByte() {
        return isInstanceOf(ByteBuffer.class);
    }

    public static <T> Matcher<T> isFloat() {
        return isInstanceOf(Float.class);
    }

    public static <T> Matcher<T> isDouble() {
        return isInstanceOf(Double.class);
    }

    public static <T> Matcher<T> isNumber() {
        return isInstanceOf(Number.class);
    }

    public static <T> Matcher<T> isMap() {
        return isInstanceOf(Map.class);
    }

    public static <T> Matcher<T> isList() {
        return isInstanceOf(List.class);
    }
}
