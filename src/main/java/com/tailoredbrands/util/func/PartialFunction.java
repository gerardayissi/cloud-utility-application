package com.tailoredbrands.util.func;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface PartialFunction<A, B> extends Function<A, Optional<B>>, Serializable {}
