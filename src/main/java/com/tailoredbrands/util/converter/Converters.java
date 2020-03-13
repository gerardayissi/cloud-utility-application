package com.tailoredbrands.util.converter;

import com.google.common.collect.Lists;
import com.tailoredbrands.util.control.Try;
import com.tailoredbrands.util.func.PartialFunction;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>The library to create converters from type A to type B</p>
 *
 * <p>An example to convert an object to its type</p>
 * <pre>
 * {@code
 *     Converter<Object, String> objectToType = match(
 *         when(isString(), success("string")),
 *         when(isInteger(), success("int")));
 *     String s = objectToType.apply("hello"); // "string"
 *     String i = objectToType.apply(5); // "int"
 * }
 * </pre>
 *
 * <p>An example to convert merge strings in a json tree (represented by maps, lists and primitives) to the length</p>
 * <pre>
 * {@code
 *
 *   public static Map<String, Object> convert(Map<String, Object> tree) {
 *      return mapValuesToLength().apply(tree);
 *   }
 *
 *   public static Converter<String, Integer> stringToLength() {
 *     return Converter.of(string -> {
 *       if (string != null) {
 *         return new Success<>(string.length());
 *       } else {
 *         return new Failure<>("value is 'null'");
 *       }
 *     });
 *   }
 *
 *   public static Converter<Object, Object> mapValuesToLength() {
 *     return match(
 *         when(isMap(), mapOverValues(lazy(() -> mapValuesToLength())).cast()),
 *         when(isList(), list(lazy(() -> mapValuesToLength())).cast()),
 *         when(isString(), stringToLength().cast()),
 *         when(any(), identity())
 *     );
 *   }
 * }
 * </pre>
 */
public class Converters {

    public static <A, B> Converter<A, B> success(B value) {
        return Converter.of(in -> Try.success(value));
    }

    public static <A, B> Converter<A, B> failure(Exception error) {
        return Converter.of(in -> Try.failure(error));
    }

    public static <A, B> Converter<A, B> failure(Function<A, Exception> error) {
        return Converter.of(in -> Try.failure(error.apply(in)));
    }

    public static <A, B> Converter<A, B> identity() {
        return Converter.of(in -> Try.success((B) in));
    }

    public static <A, B> Converter<A, B> lazy(Supplier<Converter<A, B>> lazy) {
        return Converter.of(in -> lazy.get().apply(in));
    }

    public static <A, B> Converter<A, B> or(Converter<A, B> first, Converter<A, B> second) {
        return first.recover(second);
    }

    public static <A, B, B1 extends B, B2 extends B, B3 extends B> Converter<A, B> or(Converter<A, B1> first, Converter<A, B2> second, Converter<A, B3> third) {
        return first.map(v -> (B) v).recover(second).recover(third);
    }

    public static <A, B1, B2> Converter<A, Tuple2<B1, B2>> product(Converter<A, B1> first, Converter<A, B2> second) {
        return first.product(second);
    }

    @SafeVarargs
    public static <A, B> Converter<A, ? extends Collection<? extends B>> productN(
            Converter<A, ? extends B>... converters) {
        return productN(Arrays.asList(converters));
    }

    @SuppressWarnings("unchecked")
    public static <A, B> Converter<A, ? extends Collection<? extends B>> productN(
            Collection<Converter<A, ? extends B>> converters) {
        return converters
                .stream()
                .map(converter -> converter.map(Lists::newArrayList))
                .reduce((first, second) ->
                        product(first, second)
                                .map(tuple -> tuple.apply((c1, c2) -> {
                                    c1.addAll(c2);
                                    return c1;
                                }))
                )
                .get();
    }

    public static <A, B> Converter<A, B> _null() {
        return success(null);
    }

    @SafeVarargs
    public static <A, B> Converter<A, ? extends B> match(PartialFunction<A, Converter<A, ? extends B>>... choices) {
        return match(Arrays.asList(choices));
    }

    public static <A, B> Converter<A, ? extends B> match(List<PartialFunction<A, Converter<A, ? extends B>>> choices) {
        return Converter.of(in -> {
            Converter<A, ? extends B> converter = choices.stream()
                    .map(choice -> choice.apply(in))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElseGet(() -> failure(new RuntimeException("Non-exhaustive pattern matching")));
            return converter.apply(in);
        });
    }

    public static <A, B> PartialFunction<A, Converter<A, ? extends B>> when(Predicate<A> predicate, Converter<A, ? extends B> converter) {
        return in -> {
            if (predicate.test(in)) {
                return Optional.of(converter);
            } else {
                return Optional.empty();
            }
        };
    }

    public static <A, B> Converter<List<A>, List<B>> list(Converter<A, B> itemConverter) {
        return Converter.of(in -> {
            Tuple2<io.vavr.collection.List<Try<B>>, io.vavr.collection.List<Try<B>>>
                    successAndFailure = io.vavr.collection.List.ofAll(in)
                    .map(itemConverter)
                    .partition(Try::isSuccess);
            if (successAndFailure._2.isEmpty()) {
                return Try.success(successAndFailure._1.map(Try::get).asJava());
            } else {
                return Try.failure(successAndFailure._2.map(f -> f.failed().get()).get(0));
            }
        });
    }

    public static <A, B> Converter<io.vavr.collection.List<A>, io.vavr.collection.List<B>> listV(Converter<A, B> itemConverter) {
        return Converter.of(in -> {
            Tuple2<io.vavr.collection.List<Try<B>>, io.vavr.collection.List<Try<B>>>
                    successAndFailure = in
                    .map(itemConverter)
                    .partition(Try::isSuccess);
            if (successAndFailure._2.isEmpty()) {
                return Try.success(successAndFailure._1.map(Try::get));
            } else {
                return Try.failure(successAndFailure._2.map(f -> f.failed().get()).get(0));
            }
        });
    }

    public static <K, A, B> Converter<Map<K, A>, Map<K, B>> mapOverValues(Converter<A, B> itemValuesConverter) {
        return map(tupleSecond(itemValuesConverter));
    }

    private static <K, A, B> Converter<Tuple2<K, A>, Tuple2<K, B>> tupleSecond(Converter<A, B> itemValuesConverter) {
        return Converter.of(in -> itemValuesConverter.apply(in._2).match(
                success -> Try.success(Tuple.of(in._1, success)),
                Try::failure));
    }

    public static <K, A, B> Converter<Map<K, A>, Map<K, B>> map(Converter<Tuple2<K, A>, Tuple2<K, B>> itemConverter) {
        return Converters.<Map<K, A>, Map<K, A>>identity()
                .map(Converters::mapToList)
                .join(list(itemConverter))
                .map(Converters::listToMap);
    }

    private static <K, B> Map<K, B> listToMap(List<Tuple2<K, B>> listOfTuples) {
        return listOfTuples
                .stream()
                .collect(Collectors.toMap(t -> t._1, t -> t._2));
    }

    private static <K, A> List<Tuple2<K, A>> mapToList(Map<K, A> map) {
        return map.entrySet()
                .stream()
                .map(entry -> Tuple.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
