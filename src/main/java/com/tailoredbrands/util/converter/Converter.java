package com.tailoredbrands.util.converter;


import com.tailoredbrands.util.control.Try;
import com.tailoredbrands.util.func.SerializableFunction;
import io.vavr.Tuple2;

import java.util.Optional;
import java.util.function.Function;

public class Converter<A, B> implements SerializableFunction<A, Try<B>> {

    private SerializableFunction<A, Try<B>> function;

    private Converter(Function<A, Try<B>> function) {
        this.function = function::apply;
    }

    private Converter(SerializableFunction<A, Try<B>> function) {
        this.function = function;
    }

    public static <A, B> Converter<A, B> of(Function<A, Try<B>> function) {
        return new Converter<>(function);
    }

    public static <A, B> Converter<A, B> ofThrowable(Function<A, B> function) {
        return new Converter<>(a -> Try.success(function.apply(a)));
    }

    public static <A, B> Converter<A, B> ofPartial(Function<A, Optional<B>> function) {
        return ofThrowable(function.andThen(Optional::get));
    }

    @Override
    public Try<B> apply(A in) {
        return Try.of(() -> function.apply(in)).map(Try::get);
    }

    public B applyAndGet(A in) {
        return apply(in).get();
    }

    public <B2> Converter<A, B2> flatMap(Function<B, Converter<B, B2>> mapper) {
        return of(in -> apply(in).match(success -> mapper.apply(success).apply(success), Try::failure));
    }

    public <B2> Converter<A, B2> join(Converter<B, B2> other) {
        return flatMap(ignore -> other);
    }

    public <B2> Converter<A, B2> map(Function<B, B2> mapper) {
        return flatMap(result -> Converters.success(mapper.apply(result)));
    }

    @SuppressWarnings("unchecked")
    public <A1, B1> Converter<A1, B1> cast() {
        return (Converter<A1, B1>) this;
    }

    public Converter<A, B> recover(Converter<A, ? extends B> other) {
        return of(in -> apply(in).recoverWith(e -> other.apply(in)));
    }

    public <B2> Converter<A, Tuple2<B, B2>> product(Converter<A, ? extends B2> other) {
        return Converter.of(in -> Try.zip(apply(in), other.apply(in)));
    }

    @SuppressWarnings("unchecked")
    static <A, B> Converter<A, B> narrow(Converter<A, ? extends B> t) {
        return (Converter<A, B>) t;
    }
}
