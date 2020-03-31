package com.tailoredbrands.util;


import org.apache.beam.sdk.values.KV;
import org.apache.commons.csv.CSVRecord;

import java.io.Serializable;
import java.util.List;

public class FileWithMeta implements Serializable {
    private final String sourceName;
    private final List<KV<Integer, CSVRecord>> records;
    private final String content;

    FileWithMeta(String sourceName, String content, List<KV<Integer, CSVRecord>> records) {
        this.sourceName = sourceName;
        this.content = content;
        this.records = records;
    }

    public String getSourceName() {
        return sourceName;
    }

    public List<KV<Integer, CSVRecord>> getRecords() {
        return records;
    }

    public String getFileContent() {
        return content;
    }

    @Override
    public String toString() {
        return "<" + sourceName + ":" + records + ">";
    }

    public static FileWithMeta of(String filename, String content, List<KV<Integer, CSVRecord>> records) {
        return new FileWithMeta(filename, content, records);
    }
}
