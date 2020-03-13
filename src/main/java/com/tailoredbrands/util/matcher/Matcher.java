package com.tailoredbrands.util.matcher;

import java.util.function.Predicate;

@FunctionalInterface
public interface Matcher<T> extends Predicate<T> {

    default Matcher<T> negateIf(boolean predicate) {
        return t -> {
            boolean result = test(t);
            return predicate != result;
        };
    }
}
