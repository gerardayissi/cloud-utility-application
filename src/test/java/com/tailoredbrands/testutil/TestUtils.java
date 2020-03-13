package com.tailoredbrands.testutil;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tailoredbrands.util.json.JsonUtils;
import com.tibco.tibjms.TibjmsDestination;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.io.jms.JmsRecord;

import java.io.File;
import java.util.HashMap;

public class TestUtils {

    public static PubsubMessage pubsubMessage(String payload) {
        return new PubsubMessage(payload.getBytes(), new HashMap<>());
    }

    public static JmsRecord jmsRecord(String payload) {
        return new JmsRecord("testID", System.currentTimeMillis(), "testCorrID",
                new TibjmsDestination(), new TibjmsDestination(), 1, true,
                "test", 999999999L, 1, new HashMap<String, Object>(), payload);
    }

    public static ObjectNode objectNode() {
        return JsonUtils.mapper().createObjectNode();
    }

    public static String getAbsolutePath(String resourceFile) {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        File file = new File(classLoader.getResource(resourceFile).getFile());
        return file.getAbsolutePath();
    }
}
