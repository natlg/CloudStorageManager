package com.nat.cloudman.cloud;

import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CloudManagerFacade {

    @Autowired
    private UserService userService;

    @Autowired
    private DropboxManager dropboxManager;

    @Autowired
    private UserManager userManager;

    @Autowired
    private OneDriveManager oneDriveManager;

    private Map<String, CloudManager> cloudManagers = new HashMap<>();

    @Autowired
    public void setCloudManagers(List<CloudManager> cloudManagers) {
        for (CloudManager cloudManager : cloudManagers) {
            this.cloudManagers.put(cloudManager.getServiceName(), cloudManager);
        }
    }

    public FilesContainer getFilesList(String accountName, String folderPath) {
        Cloud cloud = userManager.getCloud(accountName);
        String cloudService = cloud.getCloudService();

        return cloudManagers.get(cloudService).getFilesList(accountName, folderPath);
    }

    public void uploadFile(String cloudName, File localFile, String pathToUpload) throws Exception {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        System.out.println("uploadFile()," + " cloudService: " + cloudService);
        oneDriveManager.setRefreshToken(cloud.getRefreshToken());

        cloudManagers.get(cloudService).uploadFile(cloudName, localFile, pathToUpload);
    }

    public void addFolder(String folderName, String cloudName, String path, String parentId) {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();

        switch (cloudService) {
            case "Dropbox":
                dropboxManager.addFolder(folderName, cloudName, path);
                break;
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.addFolder(folderName, cloudName, path, parentId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
    }

    private DownloadedFileContainer download(String fileName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        switch (cloudService) {
            case "Dropbox":
                return dropboxManager.download(fileName, cloudName, path);
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.download(fileName, fileId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
        return null;
    }

    public ResponseEntity<InputStreamResource> downloadFile(String fileName, String cloudName, String fileId, String path) {
        DownloadedFileContainer fileContainer = download(fileName, cloudName, fileId, path);
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.setContentType(MediaType.parseMediaType("application/force-download"));
        byte[] arr = fileContainer.getByteArray();
        respHeaders.setContentLength(arr.length);
        respHeaders.setContentDispositionFormData("attachment", fileContainer.getName());
        InputStreamResource isr = new InputStreamResource(new ByteArrayInputStream(arr));
        return new ResponseEntity<InputStreamResource>(isr, respHeaders, HttpStatus.OK);
    }

    public void deleteFile(String fileName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        switch (cloudService) {
            case "Dropbox":
                dropboxManager.deleteFile(fileName, cloudName, path);
                break;
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.deleteFile(fileName, fileId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }
    }

    public void renameFile(String fileName, String newName, String cloudName, String fileId, String path) {

        Cloud cloud = userManager.getCloud(cloudName);
        String cloudService = cloud.getCloudService();
        switch (cloudService) {
            case "Dropbox":
                dropboxManager.renameFile(fileName, newName, cloudName, path);
                break;
            case "OneDrive":
                oneDriveManager.setRefreshToken(cloud.getRefreshToken());
                oneDriveManager.renameFile(fileName, newName, fileId);
                break;
            default:
                System.out.println(cloudService + " is not supported yet");
        }

    }

    public void copyFile(String cloudSourceName, String pathSource, String idSource, String cloudDestName, String pathDest, String idDest) {
        Cloud cloudSource = userManager.getCloud(cloudSourceName);
        String cloudServiceSource = cloudSource.getCloudService();

        if (cloudSourceName.equals(cloudDestName)) {
            switch (cloudServiceSource) {
                case "Dropbox":
                    dropboxManager.copyFile(pathSource, pathDest, cloudSourceName);
                    break;
                case "OneDrive":
                    oneDriveManager.setRefreshToken(cloudSource.getRefreshToken());
                    oneDriveManager.copyFile(pathSource, pathDest, idSource, idDest);
                    break;
                default:
                    System.out.println(cloudServiceSource + " is not supported yet");
            }
        } else {
            switch (cloudServiceSource) {
                case "Dropbox":
                    int index = pathSource.lastIndexOf("/");
                    String fileName = pathSource.substring(index + 1);
                    System.out.println(fileName + " is fileName");
                    DownloadedFileContainer fileContainer = dropboxManager.download(fileName, cloudSourceName, pathSource);
                    oneDriveManager.uploadFile(cloudDestName, new File(fileContainer.getName()), pathDest);
                    break;
                case "OneDrive":
                    break;
                default:
                    System.out.println(cloudServiceSource + " is not supported yet");


            }
        }
    }
}