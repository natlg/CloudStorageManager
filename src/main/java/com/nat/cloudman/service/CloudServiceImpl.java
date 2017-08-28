package com.nat.cloudman.service;

import com.nat.cloudman.cloud.DropboxUtils;
import com.nat.cloudman.cloud.OneDriveUtils;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.model.Role;
import com.nat.cloudman.model.User;
import com.nat.cloudman.repository.CloudRepository;
import com.nat.cloudman.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;

@Service("cloudService")
public class CloudServiceImpl implements CloudService {

    @Autowired
    private CloudRepository cloudRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private DropboxUtils dropboxUtils;

    @Autowired
    private UserManager userManager;


    @Autowired
    private CloudService cloudService;

    @Autowired
    OneDriveUtils oneDriveUtils;

    @Override
    public void saveCloud(Cloud cloud) {
        System.out.println("saveCloud");
        cloudRepository.save(cloud);
    }

    @Override
    public void addCloudToCurrentUser(String cloudDrive, String cloudName, String token) {
        switch (cloudDrive) {
            case "OneDrive":
                token = oneDriveUtils.getRefreshToken(token);
                break;
        }
        Cloud cloud = new Cloud();
        cloud.setCloudService(cloudDrive);
        cloud.setAccountName(cloudName);
        cloud.setToken(token);
        cloud.setToken(token);
        cloudService.saveCloud(cloud);
        User user = userManager.getUser();
        user.addCloud(cloud);
        userService.saveUser(user);
    }
}
