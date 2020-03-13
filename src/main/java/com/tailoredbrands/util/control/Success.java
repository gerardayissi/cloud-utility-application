package com.tailoredbrands.util.control;

import com.tailoredbrands.util.func.SerializableConsumer;
import com.tailoredbrands.util.func.SerializableFunction;
import com.tailoredbrands.util.func.SerializableFunctionE;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class Success<A> implements Try<A> {

    private final A result;

    public Success(A result) {
        this.result = result;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean isFailure() {
        return false;
    }

    @Override
    public A get() {
        return result;
    }

    @Override
    public A getOrElse(A other) {
        return result;
    }

    @Override
    public A getOrElse(SerializableFunction<? super Exception, ? extends A> other) {
        return result;
    }

    @Override
    public Try<A> orElse(Try<? extends A> other) {
        return this;
    }

    @Override
    public Success<Exception> failed() {
        return new Success<>(new UnsupportedOperationException("Success.failed"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Try<B> transform(SerializableFunction<? super A, ? extends Try<? extends B>> success,
                                SerializableFunction<? super Exception, ? extends Try<? extends B>> failure) {
        checkNotNull(success, "'failure' is required");
        try {
            return (Try<B>) success.apply(result);
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Try<B> flatMap(SerializableFunctionE<? super A, ? extends Try<? extends B>> mapper) {
        checkNotNull(mapper, "'mapper' is required");
        try {
            return (Try<B>) mapper.apply(get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    @Override
    public Try<A> recoverWith(SerializableFunctionE<? super Exception, ? extends Try<? extends A>> recovery) {
        return this;
    }

    @Override
    public Try<A> doOnSuccess(SerializableConsumer<? super A> action) {
        action.accept(result);
        return this;
    }

    @Override
    public Try<A> doOnError(SerializableConsumer<? super Exception> action) {
        return this;
    }

    @Override
    public Optional<A> toOptional() {
        return Optional.ofNullable(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Success)) return false;
        Success<?> success = (Success<?>) o;
        return Objects.equals(result, success.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }

    @Override
    public String toString() {
        return "Success{" +
                "result=" + result +
                '}';
    }
}
