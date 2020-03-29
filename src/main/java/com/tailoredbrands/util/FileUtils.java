package com.tailoredbrands.util;

public class FileUtils {

    public static String getProcessedFilePath(String filePath) {
        try {
            String pathWithoutFile = filePath.substring(0, filePath.lastIndexOf('/'));
            String fileName = filePath.substring(pathWithoutFile.length() + 1);
            String processedPath = pathWithoutFile + "/processed/";
            return processedPath + fileName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
