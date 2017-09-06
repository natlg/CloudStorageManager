package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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


    public ArrayList<HashMap<String, String>> getFilesList(String accountName, String folderPath) {
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

        }
        return null;
    }
}
