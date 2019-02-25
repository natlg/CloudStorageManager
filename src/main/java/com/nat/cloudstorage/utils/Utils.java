package com.nat.cloudstorage.utils;

import com.nat.cloudstorage.response.DownloadedFileContainer;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

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

    public static DownloadedFileContainer fileToContainer(File file, String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("downloaded ");
        try {
            byte[] arr = IOUtils.toByteArray(is);
            is.close();
            if (file.delete()) {
                System.out.println(file.getName() + " is deleted");
            } else {
                System.out.println("Delete operation is failed");
            }
            return new DownloadedFileContainer(fileName, arr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ResponseEntity<InputStreamResource> fromFileContainerToResponseEntity(DownloadedFileContainer fileContainer) {
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.parseMediaType("application/force-download"));
        byte[] arr = fileContainer.getByteArray();
        respHeaders.setContentLength(arr.length);
        respHeaders.setContentDispositionFormData("attachment", fileContainer.getName());
        InputStreamResource isr = new InputStreamResource(new ByteArrayInputStream(arr));
        return new ResponseEntity<InputStreamResource>(isr, respHeaders, HttpStatus.OK);
    }
}
