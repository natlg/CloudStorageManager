package com.nat.cloudman.cloud.copy;

import com.nat.cloudman.model.Cloud;

public interface InterCloudTask {

    void copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest);
}
