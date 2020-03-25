package com.tailoredbrands.pipeline.error;

public enum ErrorType {
    CSV_ROW_TO_OBJECT_CONVERSION_ERROR("csv_row_to_object_conversion_error"),

    JMS_PAYLOAD_EXTRACTION_ERROR("jms_payload_extraction_error"),

    JSON_TO_PUBSUB_MESSAGE_CONVERSION_ERROR("json_to_pubsub_message_conversion_error"),

    OBJECT_TO_JSON_CONVERSION_ERROR("object_to_json_conversion_error"),
    OBJECT_TO_OBJECT_CONVERSION_ERROR("object_to_object_conversion_error"),

    XML_TO_OBJECT_CONVERSION_ERROR("xml_to_object_conversion_error"),

    OTHER(null);

    private final String name;

    ErrorType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
