package com.tailoredbrands.testutil;

import com.google.common.collect.Maps;
import lombok.val;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.core.AnyOf;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class Matchers {

    @SafeVarargs
    public static <A> Void hasItems(Iterable<A> iterable, Matcher<? super A>... matchers) {
        for (A next : iterable) {
            AnyOf<A> matcher = CoreMatchers.anyOf(matchers);
            if (!matcher.matches(next)) {
                StringDescription description = new StringDescription(
                        new StringBuilder().append("Every PCollection element should satisfy one of the matchers "));
                matcher.describeTo(description);
                description.appendText(", but was " + next.toString());
                throw new AssertionError(description.toString());
            }
        }
        return null;
    }

    public static <A> Void matchAll(Iterable<A> iterable, Matcher<? super A> matcher) {
        for (A next : iterable) {
            if (!matcher.matches(next)) {
                StringDescription description = new StringDescription();
                matcher.describeTo(description);
                description.appendText(", but was ");
                matcher.describeMismatch(next, description);
                throw new AssertionError(description.toString());
            }
        }
        return null;
    }

    private static class PubsubMessageBodyMatcher extends BaseMatcher<PubsubMessage> {

        private final Matcher<String> matcher;
        private String target;

        PubsubMessageBodyMatcher(Matcher<String> matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(Object item) {
            this.target = new String(((PubsubMessage) item).getPayload());
            return matcher.matches(target);
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            matcher.describeMismatch(target, description);
        }

        @Override
        public void describeTo(Description description) {
            matcher.describeMismatch(target, description); // was
            matcher.describeTo(description.appendText(" but expected ")); // expected
        }
    }

    public static Matcher<PubsubMessage> messageWithPayload(Matcher<String> matcher) {
        return new PubsubMessageBodyMatcher(matcher);
    }

    public static Matcher<PubsubMessage> messageWithPayload(String value) {
        return new PubsubMessageBodyMatcher(CoreMatchers.is(value));
    }

    private static class ExprMatcher<A> extends BaseMatcher<A> {

        private final Predicate<A> predicate;
        private A item;

        private ExprMatcher(Predicate<A> predicate) {
            this.predicate = predicate;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean matches(Object item) {
            this.item = (A) item;
            return predicate.test((A) item);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("The value did not match the expression. Value: " + item);
        }
    }

    public static <A> Matcher<A> with(Predicate<A> predicate) {
        return new ExprMatcher<>(predicate);
    }

    private static class MapMatcher<A, B> extends BaseMatcher<A> {

        private final Function<A, B> mapper;
        private final Matcher<B> matcher;

        private MapMatcher(Function<A, B> mapper, Matcher<B> matcher) {
            this.mapper = mapper;
            this.matcher = matcher;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean matches(Object item) {
            return matcher.matches(mapper.apply((A) item));
        }

        @Override
        public void describeTo(Description description) {
            matcher.describeTo(description);
            description.appendText(" after a mapping function");
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            matcher.describeMismatch(item, description);
        }
    }

    public static <A, B> Matcher<A> map(Function<A, B> mapper, Matcher<B> matcher) {
        return new MapMatcher<>(mapper, matcher);
    }

    public static class MapContainsAll extends BaseMatcher<Map<?, ?>> {

        public static MapContainsAll mapContainsAll(Map<?, ?> expected) {
            return new MapContainsAll(expected);
        }

        private Map<?, ?> expected;
        private Map<?, ?> actual;

        public MapContainsAll(Map<?, ?> expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object object) {
            actual = (Map<?, ?>) object;
            return actual.entrySet().containsAll(expected.entrySet());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Should have 2 equal Maps");
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            val diff = Maps.difference(expected, actual);
            description.appendText(diff.toString());
        }
    }
}
