package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.service.CloudService;
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
    private DropboxUtils dropboxUtils;

    @Autowired
    private UserManager userManager;

    @Autowired
    OneDriveUtils oneDriveUtils;


    public ArrayList<HashMap<String, String>> getFilesList(String accountName, String folderPath) {
        Cloud cloud = userManager.getCloud(accountName);
        String accessToken = cloud.getAccessToken();
        String refreshToken = cloud.getRefreshToken();
        String cloudService = cloud.getCloudService();

        System.out.println("accessToken: " + accessToken + " accessToken: " + accessToken + " cloudService: " + cloudService);

        switch (cloudService) {
            case "Dropbox":
                return dropboxUtils.getFilesList(accountName, folderPath);
            case "OneDrive":
                oneDriveUtils.setAccessToken(accessToken);
                oneDriveUtils.setRefreshToken(refreshToken);
                return oneDriveUtils.getFilesList(folderPath);

        }
        return null;
    }
}
