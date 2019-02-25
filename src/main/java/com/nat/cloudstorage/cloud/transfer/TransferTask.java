package com.nat.cloudstorage.cloud.transfer;

import com.nat.cloudstorage.cloud.CloudDriveManager;
import com.nat.cloudstorage.cloud.UserManager;
import com.nat.cloudstorage.configuration.SecurityConfiguration;
import com.nat.cloudstorage.model.Cloud;
import com.nat.cloudstorage.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransferTask {

    private static final Logger logger = LoggerFactory.getLogger(TransferTask.class);

    @Autowired
    private UserManager userManager;

    private Map<String, CloudDriveManager> cloudDriveManagers = new HashMap<>();

    @Autowired
    public void setCloudDriveManagers(List<CloudDriveManager> cloudDriveManagers) {
        for (CloudDriveManager cloudDriveManager : cloudDriveManagers) {
            this.cloudDriveManagers.put(cloudDriveManager.getServiceName(), cloudDriveManager);
        }
    }

    public boolean copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest, String fileName, String parentId) {
        Cloud cloudDest = userManager.getCloud(cloudDestName);
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = Utils.getNameFromPath(pathSource);
        }
        if (!cloudSourceName.equals(cloudDestName)) {
            //need to download local and then upload to dest cloud
            Cloud cloudSource = userManager.getCloud(cloudSourceName);
            pathSource = Utils.getParentFromPath(pathSource);
            logger.debug(fileName + " is fileName, " + pathSource + " is a pathSource on copyFile");
            File file = cloudDriveManagers.get(cloudSource.getCloudService()).downloadFileLocal(fileName, pathSource, downloadUrl, idSource, cloudSource);
            return cloudDriveManagers.get(cloudDest.getCloudService()).uploadFile(cloudDest, file, pathDest, idDest);

        } else {
            // just copy inside of the same cloud
            return cloudDriveManagers.get(cloudDest.getCloudService()).copyFile(pathSource, pathDest, idSource, idDest, cloudDest, fileName, parentId);
        }
    }
}
