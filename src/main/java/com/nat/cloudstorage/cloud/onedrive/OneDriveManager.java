package com.nat.cloudstorage.cloud.onedrive;


import com.fasterxml.jackson.databind.JsonNode;
import com.nat.cloudstorage.cloud.CloudCredentials;
import com.nat.cloudstorage.cloud.CloudDriveManager;
import com.nat.cloudstorage.cloud.onedrive.client.OneDriveClient;
import com.nat.cloudstorage.cloud.onedrive.client.OneDriveConfig;
import com.nat.cloudstorage.model.Cloud;
import com.nat.cloudstorage.response.DownloadedFileContainer;
import com.nat.cloudstorage.response.FilesContainer;
import com.nat.cloudstorage.service.CloudService;
import com.nat.cloudstorage.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;


@Component
public class OneDriveManager implements CloudDriveManager {

    private static final Logger logger = LoggerFactory.getLogger(OneDriveManager.class);

    @Value("${onedrive.app.key}")
    private String APP_KEY;

    @Value("${onedrive.app.secret}")
    private String APP_SECRET;

    @Autowired
    private CloudService cloudService;

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    private OneDriveClient getClient(String accessToken, String refreshToken) {
        return new OneDriveClient(new OneDriveConfig(APP_KEY, APP_SECRET), accessToken, refreshToken);
    }

    @Override
    public String getServiceName() {
        return "OneDrive";
    }

    private void checkAndSaveAccessToken(String accessToken, Cloud cloud) {
        if (!accessToken.equals(cloud.getAccessToken())) {
            cloud.setAccessToken(accessToken);
            cloudService.saveCloud(cloud);
        }
    }

    public CloudCredentials sendAuthorizationCodeRequest(String code) {
        OneDriveClient client = getClient(null, null);
        ResponseEntity<JsonNode> response = client.sendAuthorizationCodeRequest(code);
        return new CloudCredentials(response.getBody().get("access_token").asText(), response.getBody().get("refresh_token").asText());
    }

    @Override
    public FilesContainer getFilesList(Cloud cloud, String folderId, String folderPath) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        FilesContainer result = client.getFilesList(folderPath, folderId);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public boolean uploadFile(Cloud cloud, File localFile, String filePath, String parentId) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        boolean result = client.uploadFile(localFile, filePath);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        logger.debug("uploadFile, resulet: " + result);
        return result;
    }


    public boolean uploadFolderRecursive(final File folder, Cloud cloud, String pathToUpload, String parentId) {
        logger.debug("uploadFolderRecursive, folder: " + folder.getAbsolutePath() + ", pathToUpload: " + pathToUpload);
        boolean result = true;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                logger.debug("folder: " + fileEntry.getAbsolutePath());
                String folderId = addFolderAndGet(fileEntry.getName(), cloud, pathToUpload, parentId);
                if (folderId == null) {
                    logger.debug("because failed to create folder");
                    result = false;
                }
                if (!uploadFolderRecursive(fileEntry, cloud, pathToUpload + "/" + fileEntry.getName(), folderId)) {
                    result = false;
                }
            } else {
                logger.debug("file: " + fileEntry.getAbsolutePath());
                if (!uploadFile(cloud, fileEntry, pathToUpload + "/" + fileEntry.getName(), parentId)) {
                    logger.debug("because failed to upload file");
                    result = false;
                }
            }
        }
        logger.debug("return from od uploadFolderRecursive: " + result);
        return result;
    }

    @Override
    public boolean uploadFolder(Cloud cloud, File localFolder, String pathToUpload, String parentId, String idDest) {
        logger.debug("OneDrive uploadFolder, pathToUpload: " + pathToUpload + ", idDest: " + idDest);
        String folderId = addFolderAndGet(localFolder.getName(), cloud, pathToUpload, idDest);
        return uploadFolderRecursive(localFolder, cloud, pathToUpload, folderId);
    }

    @Override
    public boolean addFolder(String folderName, Cloud cloud, String path, String parentId) {
        if (addFolderAndGet(folderName, cloud, path, parentId) != null) {
            return true;
        }
        return false;
    }

    private String addFolderAndGet(String folderName, Cloud cloud, String path, String parentId) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        String result = client.addFolder(folderName, path, parentId);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        logger.debug("addFolder return: " + result);
        return result;
    }

    @Override
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        DownloadedFileContainer result = client.download(fileName, fileId, path);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public boolean deleteFile(String fileId, String path, Cloud cloud, String parentId) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        boolean result = client.deleteFile(fileId, path);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public boolean renameFile(String fileName, String fileId, String newName, String path, Cloud cloud) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        boolean result = client.renameFile(fileName, fileId, newName, path);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public File downloadFileLocal(String fileName, String path, String downloadUrl, String fileId, Cloud cloud) {
        return downloadToLocalPath(fileName, downloadUrl, DOWNLOAD_PATH + "\\" + System.currentTimeMillis(), cloud);
    }

    private File downloadToLocalPath(String fileName, String downloadUrl, String localPath, Cloud cloud) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        File result = client.downloadLocal(fileName, downloadUrl, localPath);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud, String fileName, String parentId) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        boolean result = client.copyFile(pathSourse, pathDest, idSource, idDest);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public boolean moveFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud, String fileName, String parentId) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        boolean result = client.moveFile(pathSourse, pathDest, idSource, idDest);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public String getThumbnail(Cloud cloud, String fileId, String path) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        String result = client.getThumbnail(fileId);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
        return result;
    }

    @Override
    public DownloadedFileContainer downloadFolder(String folderName, String folderId, String path, Cloud cloud) {
        File localFolder = downloadFolderLocal(folderName, path + "/" + folderName, "", folderId, cloud);
        String zipPath = null;
        try {
            zipPath = localFolder.getCanonicalPath() + ".zip";
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("zipPath: " + zipPath);
        File zipFile = new File(zipPath);
        // unzip including root folder
        ZipUtil.pack(localFolder, zipFile, true);
        return Utils.fileToContainer(zipFile, folderName + ".zip");
    }

    @Override
    public File downloadFolderLocal(String folderName, String path, String downloadUrl, String folderId, Cloud cloud) {
        logger.debug("downloadFolderLocal, folderName: " + folderName + ", path: " + path + ", downloadUrl: " + downloadUrl + ", folderId: " + folderId);
        String pathToDownload = DOWNLOAD_PATH + "\\" + System.currentTimeMillis() + "\\" + folderName + "\\";
        logger.debug("downloadFolderLocal, pathToDownload: " + pathToDownload);
        File localFolder = downloadFolderRecursive(folderName, path, folderId, pathToDownload, cloud);
        logger.debug("return from downloadFolderLocal: " + localFolder.getAbsolutePath());
        return localFolder;
    }

    private File downloadFolderRecursive(String folderName, String path, String folderId, String pathToDownload, Cloud cloud) {
        File localFolder = new File(pathToDownload);
        //create folder
        logger.debug("downloadFolderRecursive, " + localFolder.mkdir() + localFolder.mkdirs());
        logger.debug("downloadFolderRecursive, pathToDownload: " + pathToDownload + ", folderName: " + folderName + ", path: " + path);
        FilesContainer filesContainer = getFilesList(cloud, folderId, path);
        for (HashMap<String, String> file : filesContainer.getFiles()) {
            String fileName = file.get("name");
            String fileType = file.get("type");
            String fileId = file.get("id");
            String downloadPath = file.get("downloadUrl");
            logger.debug("file: " + fileName + " " + fileType + " " + fileId);
            if (file.get("type").equalsIgnoreCase("file")) {
                downloadToLocalPath(fileName, downloadPath, pathToDownload, cloud);
            } else {
                String newLocalPath = pathToDownload + fileName + "\\";
                String newFolderPath = path + "/" + fileName;
                //repeat
                downloadFolderRecursive(fileName, newFolderPath, fileId, newLocalPath, cloud);
            }
        }
        return localFolder;
    }
}
