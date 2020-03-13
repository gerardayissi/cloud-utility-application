package com.tailoredbrands.pipeline.pattern;

import com.google.auto.service.AutoService;
import org.apache.beam.sdk.harness.JvmInitializer;

import java.util.TimeZone;

import static java.util.TimeZone.getTimeZone;

@AutoService(JvmInitializer.class)
public class OracleEnvProvider implements JvmInitializer {

    public OracleEnvProvider() {
    }

    @Override
    public void onStartup() {
        TimeZone.setDefault(getTimeZone("CST"));
    }
}
