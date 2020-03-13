package com.tailoredbrands.util.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

@FunctionalInterface
public interface Projection {

    class All implements Projection {

        private All() {}

        @Override
        public boolean acceptField(String name) {
            return true;
        }

        @Override
        public boolean isAll() {
            return true;
        }

        @Override
        public Projection union(Projection other) {
            return this;
        }

        @Override
        public Projection intersection(Projection other) {
            return other;
        }

        @Override
        public String toString() {
            return "All";
        }
    }

    class None implements Projection {

        private None() {}

        @Override
        public boolean isNone() {
            return true;
        }

        @Override
        public Projection union(Projection other) {
            return other;
        }

        @Override
        public Projection intersection(Projection other) {
            return this;
        }

        @Override
        public boolean acceptField(String name) {
            return false;
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    Projection ALL = new All();
    Projection NONE = new None();

    boolean acceptField(String name);

    default boolean isAll() {
        return false;
    }

    default boolean isNone() {
        return false;
    }

    default ObjectNode project(ObjectNode original) {
        if (isAll()) {
            return original;
        } else if (isNone()) {
            return JsonUtils.mapper().createObjectNode();
        } else {
            return JsonUtils.propertiesOf(original)
                    .stream()
                    .filter(tuple -> acceptField(tuple._1()))
                    .reduce(
                            JsonUtils.mapper().createObjectNode(),
                            (acc, next) -> (ObjectNode) acc.set(next._1(), next._2()),
                            JsonUtils::merge);
        }
    }

    default ObjectNode projectEffectful(ObjectNode original) {
        JsonUtils.propertyNamesOf(original).forEach(name -> {
            if (!acceptField(name)) {
                original.remove(name);
            }
        });
        return original;
    }

    default Projection union(Projection other) {
        if (other.isAll()) {
            return other;
        } else {
            return name -> this.acceptField(name) || other.acceptField(name);
        }
    }

    default Projection intersection(Projection other) {
        if (other.isNone()) {
            return other;
        } else {
            return name -> this.acceptField(name) && other.acceptField(name);
        }
    }

    static Projection all() {
        return ALL;
    }

    static Projection none() {
        return NONE;
    }
}
