package com.nat.cloudman.cloud.copy;

import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;

@Component
public class OneDriveToDropbox implements InterCloudTask {

    @Autowired
    private DropboxManager dropboxManager;


    @Autowired
    private OneDriveManager oneDriveManager;


    @Autowired
    private UserManager userManager;

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    @Override
    public void copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest) {
        String fileName = Utils.getNameFromPath(pathSource);
        File file = new File(DOWNLOAD_PATH + System.currentTimeMillis() + fileName);
        try {
            //TODO set timeout
            FileUtils.copyURLToFile(new URL(downloadUrl), file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Cloud cloudDest = userManager.getCloud(cloudDestName);
        dropboxManager.uploadFile(cloudDest, file, pathDest);
    }
}
