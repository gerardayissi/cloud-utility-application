package com.tailoredbrands.util.predef;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Resources {

    public static InputStream read(String path) {
        return Resources.class.getClassLoader().getResourceAsStream(path);
    }

    public static boolean exists(String path) {
        return Resources.class.getClassLoader().getResource(path) != null;
    }

    public static String readAsString(String path) {
        return new BufferedReader(new InputStreamReader(read(path)))
                .lines().collect(Collectors.joining("\n"));
    }
}
