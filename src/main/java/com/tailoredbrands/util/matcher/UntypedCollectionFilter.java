package com.tailoredbrands.util.matcher;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

public interface UntypedCollectionFilter<T> extends Function<Collection<?>, Stream<T>> {}
