package com.tailoredbrands.util.coder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.beam.sdk.coders.CoderProvider;
import org.apache.beam.sdk.coders.CoderProviderRegistrar;
import org.apache.beam.sdk.coders.CoderProviders;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.util.List;

/**
 * Registers all the TB custom coders
 */
@AutoService(CoderProviderRegistrar.class)
public class TBCoderProviderRegistrar implements CoderProviderRegistrar {
    @Override
    public List<CoderProvider> getCoderProviders() {
        return ImmutableList.of(
                CoderProviders.forCoder(TypeDescriptor.of(JsonNode.class), Coders.jsonNode()),
                CoderProviders.fromStaticMethods(Tuple2.class, Tuple2Coder.class),
                CoderProviders.fromStaticMethods(Try.class, TryCoder.class)
        );
    }
}
