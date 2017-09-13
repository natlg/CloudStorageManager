package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
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
        oneDriveManager.setRefreshToken(cloud.getRefreshToken());
        switch (cloudService) {
            case "Dropbox":
                dropboxManager.addFolder(folderName, cloudName, path);
                break;
            case "OneDrive":
                oneDriveManager.addFolder(folderName, cloudName, path, parentId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
    }
}
