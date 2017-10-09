package com.nat.cloudman.cloud;

import com.nat.cloudman.response.FilesContainer;

import java.io.File;

public interface CloudManager {

    FilesContainer getFilesList(String accountName, String folderPath);

    String getServiceName();

    void uploadFile(String cloudName, File localFile, String pathToUpload);
}
