package com.nat.cloudman.cloud.transfer;

import com.nat.cloudman.cloud.CloudManager;
import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransferTask {

    @Autowired
    private DropboxManager dropboxManager;

    @Autowired
    private OneDriveManager oneDriveManager;

    @Autowired
    private UserManager userManager;

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    private Map<String, CloudManager> cloudManagers = new HashMap<>();

    @Autowired
    public void setCloudManagers(List<CloudManager> cloudManagers) {
        for (CloudManager cloudManager : cloudManagers) {
            this.cloudManagers.put(cloudManager.getServiceName(), cloudManager);
        }
    }

    public void copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest) {
        Cloud cloudDest = userManager.getCloud(cloudDestName);
        if (!cloudSourceName.equals(cloudDestName)) {
            //need to download local and then upload to dest cloud
            Cloud cloudSource = userManager.getCloud(cloudSourceName);
            String fileName = Utils.getNameFromPath(pathSource);
            pathSource = Utils.getParentFromPath(pathSource);
            System.out.println(fileName + " is fileName, " + pathSource + " is a pathSource");

            File file = cloudManagers.get(cloudSource.getCloudService()).downloadLocal(fileName, pathSource, downloadUrl, cloudSource);
            cloudManagers.get(cloudDest.getCloudService()).uploadFile(cloudDest, file, pathDest);

        } else {
            // just copy inside of the same cloud
            cloudManagers.get(cloudDest.getCloudService()).copyFile(pathSource, pathDest, idSource, idDest, cloudDest);
        }
    }
}