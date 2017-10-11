package com.nat.cloudman.cloud.copy;

import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.model.Cloud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DropboxToDropbox implements InterCloudTask {
    @Autowired
    private DropboxManager dropboxManager;


    @Override
    public void copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest) {
        //TODO for different accounts
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (cloudSourceName.equals(cloudDestName)) {
            dropboxManager.copyFile(pathSource, pathDest, cloudSourceName);
        }

    }
}
