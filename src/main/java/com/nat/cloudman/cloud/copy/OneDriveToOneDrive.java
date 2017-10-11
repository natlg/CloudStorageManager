package com.nat.cloudman.cloud.copy;

import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.model.Cloud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OneDriveToOneDrive implements InterCloudTask {

    @Autowired
    private DropboxManager dropboxManager;


    @Autowired
    private OneDriveManager oneDriveManager;

    @Autowired
    private UserManager userManager;

    @Override
    public void copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest) {
        //TODO dif clouds
        Cloud cloudSource = userManager.getCloud(cloudSourceName);
        oneDriveManager.setRefreshToken(cloudSource.getRefreshToken());
        oneDriveManager.copyFile(pathSource, pathDest, idSource, idDest);
    }
}
