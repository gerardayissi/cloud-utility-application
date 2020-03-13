package com.tailoredbrands.pipeline.options;

public enum JmsConnectionProvider {
    TIBCO("tibco"),
    ACTIVEMQ("activemq"),
    WEBSPHERE("websphere");

    private final String provider;

    JmsConnectionProvider(String provider) {
        this.provider = provider;
    }

    public String getProvider() {
        return this.provider;
    }
}
