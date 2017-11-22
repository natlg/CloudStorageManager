package com.nat.cloudman.cloud;


import com.fasterxml.jackson.databind.JsonNode;
import com.nat.cloudman.cloud.nat.onedrive.client.OneDriveClient;
import com.nat.cloudman.cloud.nat.onedrive.client.OneDriveConfig;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
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
    private UserManager userManager;

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    private OneDriveClient getClient(String refreshToken) {
        return new OneDriveClient(new OneDriveConfig(APP_KEY, APP_SECRET), refreshToken);
    }

    @Override
    public String getServiceName() {
        return "OneDrive";
    }

    public ResponseEntity<JsonNode> sendAuthorizationCodeRequest(String code) {
        OneDriveClient client = getClient(null);
        return client.sendAuthorizationCodeRequest(code);
    }

    @Override
    public FilesContainer getFilesList(String accountName, String folderPath) {
        Cloud cloud = userManager.getCloud(accountName);
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        return client.getFilesList(folderPath);
    }

    @Override
    public boolean uploadFile(Cloud cloud, File localFile, String filePath) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        return client.uploadFile(localFile, filePath);
    }

    @Override
    public void addFolder(String folderName, Cloud cloud, String path, String parentId) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        client.addFolder(folderName, path, parentId);
    }

    @Override
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        return client.download(fileName, fileId, path);
    }

    @Override
    public void deleteFile(String fileId, String path, Cloud cloud) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        client.deleteFile(fileId, path);
    }

    @Override
    public void renameFile(String fileName, String fileId, String newName, String path, Cloud cloud) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        client.renameFile(fileName, fileId, newName, path);
    }

    @Override
    public File downloadLocal(String fileName, String path, String downloadUrl, Cloud cloud) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        return client.downloadLocal(fileName, path, downloadUrl, DOWNLOAD_PATH);
    }

    @Override
    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud) {
        String refreshToken = cloud.getRefreshToken();
        OneDriveClient client = getClient(refreshToken);
        return client.copyFile(pathSourse, pathDest, idSource, idDest);
    }
}
