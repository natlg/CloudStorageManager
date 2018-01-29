package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;

import java.io.File;

public interface CloudManager {

    FilesContainer getFilesList(Cloud cloud, String folderId, String folderPath);

    String getServiceName();

    boolean uploadFile(Cloud cloud, File localFile, String pathToUpload);

    boolean addFolder(String folderName, Cloud cloud, String path, String parentId);

    DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud);

    boolean deleteFile(String fileId, String path, Cloud cloud);

    boolean renameFile(String fileName, String fileId, String newName, String path, Cloud cloud);

    File downloadLocal(String fileName, String path, String downloadUrl, Cloud cloud);

    boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud);

    String getThumbnail(Cloud cloud, String fileId, String path);
}
