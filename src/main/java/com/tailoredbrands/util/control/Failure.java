package com.tailoredbrands.util.control;

import com.tailoredbrands.util.func.SerializableConsumer;
import com.tailoredbrands.util.func.SerializableFunction;
import com.tailoredbrands.util.func.SerializableFunctionE;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class Failure<A> implements Try<A> {

    private final Exception exception;

    public Failure(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isFailure() {
        return true;
    }

    @Override
    public A get() {
        return sneakyThrow(exception);
    }

    @Override
    public A getOrElse(A other) {
        return other;
    }

    @Override
    public A getOrElse(SerializableFunction<? super Exception, ? extends A> other) {
        return other.apply(exception);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<A> orElse(Try<? extends A> other) {
        return (Try<A>) other;
    }

    @Override
    public Success<Exception> failed() {
        return new Success<>(exception);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Try<B> transform(SerializableFunction<? super A, ? extends Try<? extends B>> success,
                                SerializableFunction<? super Exception, ? extends Try<? extends B>> failure) {
        checkNotNull(failure, "'failure' is required");
        try {
            return (Try<B>) failure.apply(exception);
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Try<B> flatMap(SerializableFunctionE<? super A, ? extends Try<? extends B>> mapper) {
        return (Try<B>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<A> recoverWith(SerializableFunctionE<? super Exception, ? extends Try<? extends A>> recovery) {
        checkNotNull(recovery, "'failure' is required");
        try {
            return (Try<A>) recovery.apply(exception);
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    @Override
    public Try<A> doOnSuccess(SerializableConsumer<? super A> action) {
        return this;
    }

    @Override
    public Try<A> doOnError(SerializableConsumer<? super Exception> action) {
        action.accept(exception);
        return this;
    }

    @Override
    public Optional<A> toOptional() {
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Exception, R> R sneakyThrow(Exception t) throws T {
        throw (T) t;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Failure)) return false;
        Failure<?> failure = (Failure<?>) o;
        return Objects.equals(exception, failure.exception);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exception);
    }

    @Override
    public String toString() {
        return "Failure{" +
                "exception=" + exception +
                '}';
    }
}

