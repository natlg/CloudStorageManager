package com.nat.cloudstorage.cloud;

import com.nat.cloudstorage.model.Cloud;
import com.nat.cloudstorage.response.DownloadedFileContainer;
import com.nat.cloudstorage.response.FilesContainer;

import java.io.File;

public interface CloudDriveManager {

    FilesContainer getFilesList(Cloud cloud, String folderId, String folderPath);

    String getServiceName();

    boolean uploadFile(Cloud cloud, File localFile, String pathToUpload, String parentId);

    boolean uploadFolder(Cloud cloud, File localFolder, String pathToUpload, String parentId, String idDest);

    boolean addFolder(String folderName, Cloud cloud, String path, String parentId);

    DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud);

    boolean deleteFile(String fileId, String path, Cloud cloud, String parentId);

    boolean renameFile(String fileName, String fileId, String newName, String path, Cloud cloud);

    File downloadFileLocal(String fileName, String path, String downloadUrl, String fileId, Cloud cloud);

    File downloadFolderLocal(String folderName, String path, String downloadUrl, String folderId, Cloud cloud);

    DownloadedFileContainer downloadFolder(String folderName, String folderId, String path, Cloud cloud);

    boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud, String fileName, String parentId);

    boolean moveFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud, String fileName, String parentId);

    String getThumbnail(Cloud cloud, String fileId, String path);


}
