package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;

import java.io.File;

public interface CloudManager {

    FilesContainer getFilesList(String accountName, String folderPath);

    String getServiceName();

    void uploadFile(Cloud cloud, File localFile, String pathToUpload);

    void addFolder(String folderName, Cloud cloud, String path, String parentId);

    DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud);

    void deleteFile(String fileName, String fileId, String path, Cloud cloud);

    void renameFile(String fileName, String fileId, String newName, String path, Cloud cloud);

    File downloadLocal(String fileName, String path, String downloadUrl, Cloud cloud);

    void copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud);
}
