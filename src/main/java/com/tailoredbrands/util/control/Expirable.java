package com.tailoredbrands.util.control;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class Expirable<A> {

    private final A value;
    private final Instant expiresAt;

    private Expirable(A token, Instant expiresAt) {
        this.value = checkNotNull(token, "'value' is required'");
        this.expiresAt = checkNotNull(expiresAt, "'expiresAt' is required'");
    }

    public static <A> Expirable<A> of(A token, Instant expiresAt) {
        return new Expirable<>(token, expiresAt);
    }

    public static <B> Supplier<B> refreshing(Supplier<Expirable<B>> producer) {
        return earlyRefreshing(producer, Duration.ZERO);
    }

    public static <B> Supplier<B> earlyRefreshing(Supplier<Expirable<B>> producer, Duration timeBuffer) {
        AtomicReference<Expirable<B>> cache = new AtomicReference<>();
        return () -> {
            Expirable<B> expirable = cache.updateAndGet(prev -> {
                if (prev == null || prev.getExpiresAt().isBefore(Instant.now().plus(timeBuffer))) {
                    return producer.get();
                } else {
                    return prev;
                }
            });
            return expirable.getValue();
        };
    }

    public A getValue() {
        return value;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "Expirable{" +
                "value='" + value + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expirable)) return false;
        Expirable idToken = (Expirable) o;
        return Objects.equals(value, idToken.value) &&
                Objects.equals(expiresAt, idToken.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, expiresAt);
    }
}
