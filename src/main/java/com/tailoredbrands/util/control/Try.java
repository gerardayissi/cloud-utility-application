package com.tailoredbrands.util.control;

import com.tailoredbrands.util.func.SerializableBiFunction;
import com.tailoredbrands.util.func.SerializableConsumer;
import com.tailoredbrands.util.func.SerializableConsumerE;
import com.tailoredbrands.util.func.SerializableFunction;
import com.tailoredbrands.util.func.SerializableFunctionE;
import com.tailoredbrands.util.func.SerializableRunnableE;
import com.tailoredbrands.util.func.SerializableSupplierE;
import com.tailoredbrands.util.predef.Streams;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public interface Try<A> {

    static <B> Try<B> success(B result) {
        return new Success<>(result);
    }

    static <B> Try<B> failure(Exception exception) {
        return new Failure<>(exception);
    }

    static <B> Try<B> of(SerializableSupplierE<? extends B> supplier) {
        checkNotNull(supplier, "'supplier' is required");
        try {
            return new Success<>(supplier.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    static Try<Void> of(SerializableRunnableE runnable) {
        checkNotNull(runnable, "'runnable' is required");
        try {
            runnable.run();
            return new Success<>(null);
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    static <A, B> Try<Tuple2<A, B>> zip(Try<? extends A> first, Try<? extends B> second) {
        return zipWith(first, second, Tuple::of);
    }

    static <A, B, C> Try<C> zipWith(Try<A> first, Try<B> second, SerializableBiFunction<A, B, C> fun) {
        return first.flatMap(fv -> second.map(sv -> fun.apply(fv, sv)));
    }

    static <B> Try<B> flatten(Try<Try<B>> tryInTry) {
        return tryInTry.match(success -> success, Try::failure);
    }

    static <B> List<Try<B>> liftToList(Try<? extends Iterable<B>> values) {
        return values.match(
                success -> Streams.fromIterable(success).map(Try::success).collect(Collectors.toList()),
                failure -> Collections.singletonList(Try.failure(failure)));
    }

    static <A> Try<Collection<A>> sequence(Collection<Try<A>> collection) {
        return sequence(collection.stream()).map(stream -> stream.collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    static <A> Try<Stream<A>> sequence(Stream<Try<A>> stream) {
        Map<Boolean, List<Try<? extends A>>> groups = stream.collect(Collectors.groupingBy(Try::isSuccess));
        if (groups.containsKey(false)) {
            // first failure pops up, idiomatic way to use recursive flatMap, but may cause OOM
            return (Failure<Stream<A>>) groups.get(false).get(0);
        } else if (groups.containsKey(true)) {
            return Try.success(groups.get(true).stream().map(Try::get));
        } else {
            return Try.success(Stream.empty());
        }
    }

    static <A> Try<io.vavr.collection.List<A>> sequence(io.vavr.collection.List<Try<A>> list) {
        return list.foldLeft(
                Try.success(io.vavr.collection.List.empty()),
                (acc, next) -> acc.flatMap(accList -> next.map(accList::append)));
    }

    static <A, B> SerializableFunction<A, Try<B>> lift(SerializableFunction<A, B> func) {
        checkNotNull(func, "'func' is required");
        return input -> {
            try {
                return new Success<>(func.apply(input));
            } catch (Exception e) {
                return new Failure<>(e);
            }
        };
    }

    boolean isSuccess();

    boolean isFailure();

    A get();

    A getOrElse(A other);

    A getOrElse(SerializableFunction<? super Exception, ? extends A> other);

    Try<A> orElse(Try<? extends A> other);

    Success<Exception> failed();

    <B> Try<B> transform(SerializableFunction<? super A, ? extends Try<? extends B>> success, SerializableFunction<? super Exception, ? extends Try<? extends B>> failure);

    default <B> B fold(SerializableFunction<? super A, ? extends B> success, SerializableFunction<? super Exception, ? extends B> failure) {
        return transform(
                result -> Try.success(success.apply(result)),
                e -> Try.success(failure.apply(e)))
                .get();
    }

    default <B> B match(SerializableFunction<? super A, ? extends B> success, SerializableFunction<? super Exception, ? extends B> failure) {
        return fold(success, failure);
    }

    <B> Try<B> flatMap(SerializableFunctionE<? super A, ? extends Try<? extends B>> mapper);

    default <B> Try<B> map(SerializableFunctionE<? super A, ? extends B> mapper) {
        checkNotNull(mapper, "'mapper' is required");
        return flatMap(value -> success(mapper.apply(value)));
    }

    default Try<A> peek(SerializableConsumerE<A> func) {
        checkNotNull(func, "'func' is required");
        return map(value -> {
            func.accept(value);
            return value;
        });
    }

    Try<A> recoverWith(SerializableFunctionE<? super Exception, ? extends Try<? extends A>> recovery);

    default Try<A> mapFailed(SerializableFunctionE<? super Exception, ? extends Exception> recovery) {
        return transform(Try::success, e -> {
            try {
                return Try.failure(recovery.apply(e));
            } catch (Exception e2) {
                return Try.failure(e2);
            }
        });
    }

    default Try<A> recover(SerializableFunctionE<? super Exception, ? extends A> recovery) {
        return recoverWith(e -> success(recovery.apply(e)));
    }

    Try<A> doOnSuccess(SerializableConsumer<? super A> action);

    Try<A> doOnError(SerializableConsumer<? super Exception> action);

    Optional<A> toOptional();

    @SuppressWarnings("unchecked")
    static <T> Try<T> narrow(Try<? extends T> t) {
        return (Try<T>) t;
    }

}
