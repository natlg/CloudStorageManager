package com.nat.cloudman.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public class Utils {
    public static String getNameFromPath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            int index = path.lastIndexOf("/");
            return path.substring(index + 1);
        }
        return null;
    }

    public static String getParentFromPath(String path) {
        int index = path.lastIndexOf("/");
        return path.substring(0, index);
    }

    public static File multipartToFile(MultipartFile multipart, String pathToSave) throws IllegalStateException, IOException {
        File convertedFile = new File(pathToSave + multipart.getOriginalFilename());
        if (!convertedFile.exists()) {
            convertedFile.createNewFile();
            System.out.println("start creating new file: " + convertedFile.getPath());
        }

        multipart.transferTo(convertedFile);
        System.out.println("converted, exists " + convertedFile.exists() + ", getPath " + convertedFile.getPath()
                + ", getName " + convertedFile.getName() + ", length: " + convertedFile.length());
        return convertedFile;
    }
}
