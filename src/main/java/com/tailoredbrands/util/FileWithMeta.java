package com.tailoredbrands.util;

import io.vavr.collection.List;

import java.io.Serializable;
import java.util.Map;

public class FileWithMeta implements Serializable {
    private final String sourceName;
    private final List<Map<String, String>> records;
    private final String content;

    FileWithMeta(String sourceName, String content, List<Map<String, String>> records) {
        this.sourceName = sourceName;
        this.content = content;
        this.records = records;
    }

    public String getSourceName() {
        return sourceName;
    }

    public List<Map<String, String>> getRecords() {
        return records;
    }

    public String getFileContent() {
        return content;
    }

    @Override
    public String toString() {
        return "<" + sourceName + ":" + records + ">";
    }

    public static FileWithMeta of(String filename, String content, List<Map<String, String>> records) {
        return new FileWithMeta(filename, content, records);
    }
}
