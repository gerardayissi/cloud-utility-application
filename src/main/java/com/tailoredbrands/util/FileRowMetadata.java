package com.tailoredbrands.util;

import java.io.Serializable;
import java.util.Map;

public class FileRowMetadata implements Serializable {
    private final String sourceName;
    private final Map<String, String> records;

    FileRowMetadata(String sourceName, Map<String, String> records) {
        this.sourceName = sourceName;
        this.records = records;
    }

    public String getSourceName() {
        return sourceName;
    }

//    public long getRowsCnt() {
//        return rowsCnt;
//    }
    public Map<String, String> getRecord() {
        return records;
    }

    @Override
    public String toString() {
        return "<" + sourceName + ":" + records + ">";
    }

//    public static Metadata of(String filename, long rowsCnt) {
//        return new Metadata(filename, rowsCnt);
//    }
    public static FileRowMetadata of(String filename, Map<String, String> records) {
        return new FileRowMetadata(filename, records);
    }
}
