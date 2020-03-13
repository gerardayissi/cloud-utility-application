package com.tailoredbrands.util.matcher;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class UntypedCollectionFilters {

    private static <T> Stream<T> select(Collection<?> facts, Class<T> clazz) {
        return facts.stream().filter(clazz::isInstance).map(clazz::cast);
    }

    public static <T> UntypedCollectionFilter<T> select(Class<T> clazz) {
        return facts -> select(facts, clazz);
    }

    public static <T> UntypedCollectionFilter<T> selectWhere(Class<T> clazz, Predicate<T> predicate) {
        return facts -> select(facts, clazz).filter(predicate);
    }
}
