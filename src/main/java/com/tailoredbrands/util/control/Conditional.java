package com.tailoredbrands.util.control;

import com.tailoredbrands.util.func.PartialFunction;
import com.tailoredbrands.util.func.SerializableConsumer;
import com.tailoredbrands.util.func.SerializableFunction;
import com.tailoredbrands.util.func.SerializablePredicate;
import com.tailoredbrands.util.func.SerializableRunnable;
import org.joda.time.Duration;
import org.joda.time.Instant;

import java.io.Serializable;
import java.util.Optional;

/**
 * Functionality to run specified action only when given condition holds true.
 * This means that not all calls to {@link #apply(Object)} (or {@link #run()})
 * will execute specified {@link #action} (only when {@link #condition} is true)
 * <p/>
 * For example:
 * <pre>{@code
 * class LoggingFN extends DoFn<String, String> {
 *
 *   final Conditional<String, Void> conditionalLog = Conditional
 *       .action((String element) -> LOG.info("Current element: " + element))
 *       .every(100);
 *
 *   @ProcessElement
 *   public void processElement(@Element String element, OutputReceiver<String> out) {
 *     // log every 100-th element processed by the current DoFn
 *     conditionalLog.apply(element);
 *
 *     out.output(element);
 *   }
 * }
 * }</pre>
 * <p/>
 * Please note that if conditions are stateful you cannot call it in fluent (chain) manner.
 * This is <b>wrong</b>: {@code Conditional.action(() -> LOG.info("...")).every(10).run()} - instead
 * create single instance variable and call {@link #apply(T)} or {@link #run()} on it.
 */
public class Conditional<T, R> implements PartialFunction<T, R>, Serializable {

    /**
     * Action to execute only when specified {@link #condition} returns true
     */
    private final SerializableFunction<? super T, ? extends R> action;
    /**
     * Condition to check whether we should execute specified {@link #action} on {@link #apply(T)} call
     */
    private final SerializablePredicate<? super T> condition;

    private Conditional(SerializableFunction<? super T, ? extends R> action, SerializablePredicate<? super T> condition) {
        this.action = action;
        this.condition = condition;
    }

    @Override
    public Optional<R> apply(T t) {
        if (condition.test(t)) {
            return Optional.ofNullable(action.apply(t));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Call current action without specifying input value (call {@link #apply(T)} with null).
     * Convenient when current action is runnable and condition doesn't require input.
     */
    public void run() {
        apply(null);
    }

    /**
     * Build new conditional action from specified consumer
     *
     * @param action - action to run on condition
     * @return builder
     */
    public static <A, B> Builder<A, B> action(SerializableFunction<? super A, ? extends B> action) {
        return new Builder<>(action);
    }

    /**
     * Build new conditional action from specified consumer
     *
     * @param action - action to run on condition
     * @return builder
     */
    public static <A> Builder<A, Void> action(SerializableConsumer<? super A> action) {
        return new Builder<>(t -> {
            action.accept(t);
            return null;
        });
    }

    /**
     * Build new conditional action from specified runnable
     *
     * @param action - action to run on condition
     * @return builder
     */
    public static Builder<Void, Void> action(SerializableRunnable action) {
        return new Builder<>(ignored -> {
            action.run();
            return null;
        });
    }

    public static class Builder<T, R> {

        private final SerializableFunction<? super T, ? extends R> action;

        Builder(SerializableFunction<? super T, ? extends R> action) {
            this.action = action;
        }

        /**
         * Build action that will be executed when specified condition holds true
         *
         * @param condition - action condition
         * @return conditional action
         */
        public Conditional<T, R> when(SerializablePredicate<? super T> condition) {
            return new Conditional<>(action, condition);
        }

        /**
         * Run specified action only on every n-th call to {@link #apply(T)}.
         * <p/>
         * Note that this condition is stateful and you cannot call {@link #apply(T)} directly after #every.
         * Instead use instance variable (or outside of the loop scope).
         *
         * @param n - number of calls between action executions
         * @return conditional action
         */
        public Conditional<T, R> every(int n) {
            return new Conditional<>(action, new Counter<>(n));
        }

        /**
         * Run specified action not more often than once during specified period.
         * <p/>
         * Note that this is not a Scheduler! We just ensure that action (triggered by {@link #apply(T)})
         * is not executed way too often.
         * <p/>
         * Note that this condition is stateful and you cannot call {@link #apply(T)} directly after #every.
         * Instead use instance variable (or outside of the loop scope).
         *
         * @param duration - duration between action executions
         * @return conditional action
         */
        public Conditional<T, R> every(Duration duration) {
            return new Conditional<>(action, new Timer<>(duration));
        }
    }

    /**
     * Execute action on every n-th call
     */
    private static class Counter<T> implements SerializablePredicate<T> {

        /**
         * Number of calls between action execution
         */
        private final int n;
        /**
         * Current call count
         */
        private int count = 0;

        private Counter(int n) {
            this.n = n;
        }

        @Override
        public boolean test(T t) {
            count++;
            if (count == n) {
                count = 0;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Execute action only once during specified period
     */
    private static class Timer<T> implements SerializablePredicate<T> {

        /**
         * Minimal duration between action execution
         */
        private final Duration duration;
        /**
         * Last action timestamp
         */
        private Instant current = Instant.now();

        private Timer(Duration duration) {
            this.duration = duration;
        }

        @Override
        public boolean test(T t) {
            Instant now = Instant.now();
            if (now.isAfter(current.plus(duration))) {
                current = now;
                return true;
            } else {
                return false;
            }
        }
    }
}
