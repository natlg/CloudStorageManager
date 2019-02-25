package com.nat.cloudstorage.service;

import com.nat.cloudstorage.cloud.CloudCredentials;
import com.nat.cloudstorage.cloud.onedrive.OneDriveManager;
import com.nat.cloudstorage.cloud.UserManager;
import com.nat.cloudstorage.cloud.google.GoogleDriveManager;
import com.nat.cloudstorage.model.Cloud;
import com.nat.cloudstorage.model.User;
import com.nat.cloudstorage.repository.CloudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private GoogleDriveManager googleManager;

    private static final Logger logger = LoggerFactory.getLogger(CloudServiceImpl.class);

    @Override
    public void saveCloud(Cloud cloud) {
        logger.debug("saveCloud");
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
                logger.debug(cloudDrive + " is not supported yet");
                return;
        }
        logger.debug("cloudDrive: " + cloudDrive + ", " + cloudCredentials);
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
