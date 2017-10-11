package com.nat.cloudman.utils;

public class Utils {
    public static String getNameFromPath(String path) {
        int index = path.lastIndexOf("/");
        return path.substring(index + 1);
    }

    public static String getParentFromPath(String path) {
        int index = path.lastIndexOf("/");
        return path.substring(0, index);
    }
}
