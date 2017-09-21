package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

@Component
public class CloudManager {

    @Autowired
    private UserService userService;

    @Autowired
    private DropboxManager dropboxManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    OneDriveManager oneDriveManager;


    public FilesContainer getFilesList(String accountName, String folderPath) {
        Cloud cloud = userManager.getCloud(accountName);
        String accessToken = cloud.getAccessToken();
        String refreshToken = cloud.getRefreshToken();
        String cloudService = cloud.getCloudService();

        System.out.println("accessToken: " + accessToken + " accessToken: " + accessToken + " cloudService: " + cloudService);

        switch (cloudService) {
            case "Dropbox":
                return dropboxManager.getFilesList(accountName, folderPath);
            case "OneDrive":
                oneDriveManager.setAccessToken(accessToken);
                oneDriveManager.setRefreshToken(refreshToken);
                return oneDriveManager.getFilesList(folderPath);
            default:
                System.out.println(cloudService + " is not supported yet");
        }
        return null;
    }

    public void uploadFile(String cloudName, File localFile, String pathToUpload) throws Exception {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        System.out.println("uploadFile()," + " cloudService: " + cloudService);
        oneDriveManager.setRefreshToken(cloud.getRefreshToken());

        switch (cloudService) {
            case "Dropbox":
                dropboxManager.uploadFile(cloudName, localFile, pathToUpload);
                break;
            case "OneDrive":
                oneDriveManager.uploadFile(cloudName, localFile, pathToUpload);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
    }

    public void addFolder(String folderName, String cloudName, String path, String parentId) {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();

        switch (cloudService) {
            case "Dropbox":
                dropboxManager.addFolder(folderName, cloudName, path);
                break;
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.addFolder(folderName, cloudName, path, parentId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
    }

    public DownloadedFileContainer download(String fileName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        switch (cloudService) {
            case "Dropbox":
                return dropboxManager.download(fileName, cloudName, path);
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.download(fileName, fileId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
        return null;
    }

    public ResponseEntity<InputStreamResource> downloadFile(String fileName, String cloudName, String fileId, String path) {
        DownloadedFileContainer fileContainer = download(fileName, cloudName, fileId, path);
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.parseMediaType("application/force-download"));
        byte[] arr = fileContainer.getByteArray();
        respHeaders.setContentLength(arr.length);
        respHeaders.setContentDispositionFormData("attachment", fileContainer.getName());
        InputStreamResource isr = new InputStreamResource(new ByteArrayInputStream(arr));
        return new ResponseEntity<InputStreamResource>(isr, respHeaders, HttpStatus.OK);
    }

    public void deleteFile(String fileName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        switch (cloudService) {
            case "Dropbox":
                dropboxManager.deleteFile(fileName, cloudName, path);
                break;
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.deleteFile(fileName, fileId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
    }
}
