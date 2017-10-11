package com.nat.cloudman.cloud.copy;

import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DropboxToOneDrive implements InterCloudTask {

    @Autowired
    private DropboxManager dropboxManager;


    @Autowired
    private OneDriveManager oneDriveManager;


    @Autowired
    private UserManager userManager;

    @Override
    public void copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest) {
        Cloud cloudDest = userManager.getCloud(cloudDestName);
        oneDriveManager.setRefreshToken(cloudDest.getRefreshToken());
        Cloud cloudSource = userManager.getCloud(cloudSourceName);
        String fileName = Utils.getNameFromPath(pathSource);
        pathSource = Utils.getParentFromPath(pathSource);
        System.out.println(fileName + " is fileName, " + pathSource + " is a pathSource");
        File file = dropboxManager.downloadLocal(fileName, pathSource, cloudSource);
        oneDriveManager.uploadFile(userManager.getCloud(cloudDestName), file, pathDest);
    }


}
