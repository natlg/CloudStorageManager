package com.nat.cloudman.cloud.onedrive;


import com.fasterxml.jackson.databind.JsonNode;
import com.nat.cloudman.cloud.CloudCredentials;
import com.nat.cloudman.cloud.CloudManager;
import com.nat.cloudman.cloud.onedrive.client.OneDriveClient;
import com.nat.cloudman.cloud.onedrive.client.OneDriveConfig;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.service.CloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;


@Component
public class OneDriveManager implements CloudManager {

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
        return result;
    }

    @Override
    public boolean addFolder(String folderName, Cloud cloud, String path, String parentId) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        boolean result = client.addFolder(folderName, path, parentId);
        checkAndSaveAccessToken(client.getAccessToken(), cloud);
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
    public File downloadLocal(String fileName, String path, String downloadUrl, String fileId, Cloud cloud) {
        OneDriveClient client = getClient(cloud.getAccessToken(), cloud.getRefreshToken());
        File result = client.downloadLocal(fileName, path, downloadUrl, DOWNLOAD_PATH);
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
}
