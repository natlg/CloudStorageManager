package com.nat.cloudman.service;

import com.nat.cloudman.cloud.CloudCredentials;
import com.nat.cloudman.cloud.onedrive.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.cloud.google.GoogleManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.model.User;
import com.nat.cloudman.repository.CloudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("cloudService")
public class CloudServiceImpl implements CloudService {

    // @Qualifier("")
    @Autowired
    private CloudRepository cloudRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserManager userManager;

    @Autowired
    OneDriveManager oneDriveManager;

    @Autowired
    private GoogleManager googleManager;

    @Override
    public void saveCloud(Cloud cloud) {
        System.out.println("saveCloud");
        cloudRepository.save(cloud);
    }

    @Override
    public void addCloudToCurrentUser(String cloudDrive, String cloudName, String code) {
        CloudCredentials cloudCredentials;
        switch (cloudDrive) {
            case "OneDrive":
                cloudCredentials = oneDriveManager.sendAuthorizationCodeRequest(code);
                break;
            case "Dropbox":
                cloudCredentials = new CloudCredentials(code, code);
                break;
            case "Google Drive":
                cloudCredentials = googleManager.sendAuthorizationCodeRequest(code);
                break;
            default:
                System.out.println(cloudDrive + " is not supported yet");
                return;
        }
        System.out.println("cloudDrive: " + cloudDrive + ", " + cloudCredentials);
        Cloud cloud = new Cloud();
        cloud.setCloudService(cloudDrive);
        cloud.setAccountName(cloudName);
        cloud.setAccessToken(cloudCredentials.getAccessToken());
        cloud.setRefreshToken(cloudCredentials.getRefreshToken());
        saveCloud(cloud);
        User user = userManager.getUser();
        user.addCloud(cloud);
        userService.saveUser(user);
    }

    @Override
    public void removeCloud(String cloudName) {
        Cloud cloud = userManager.getCloud(cloudName);
        User user = userManager.getUser();
        user.deleteCloud(cloud);
        userService.saveUser(user);
    }
}
