package com.nat.cloudman.cloud;

import com.nat.cloudman.cloud.transfer.TransferTask;
import com.nat.cloudman.controllers.params.FileParameters;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CloudManagerFacade {


    @Autowired
    private TransferTask transferTask;


    @Autowired
    private UserManager userManager;

    private Map<String, CloudManager> cloudManagers = new HashMap<>();

    @Autowired
    public void setCloudManagers(List<CloudManager> cloudManagers) {
        for (CloudManager cloudManager : cloudManagers) {
            this.cloudManagers.put(cloudManager.getServiceName(), cloudManager);
        }
    }

    public FilesContainer getFilesList(String accountName, String folderId, String folderPath) {
        Cloud cloud = userManager.getCloud(accountName);
        return cloudManagers.get(cloud.getCloudService()).getFilesList(cloud, folderId, folderPath);
    }

    public void uploadFile(String cloudName, File localFile, String pathToUpload, String parentId) throws Exception {
        System.out.println("uploadFile(),");
        Cloud cloud = userManager.getCloud(cloudName);
        cloudManagers.get(cloud.getCloudService()).uploadFile(cloud, localFile, pathToUpload, parentId);
    }

    public void addFolder(String folderName, String cloudName, String path, String parentId) {
        Cloud cloud = userManager.getCloud(cloudName);
        cloudManagers.get(cloud.getCloudService()).addFolder(folderName, cloud, path, parentId);
    }

    private DownloadedFileContainer download(String fileName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        return cloudManagers.get(cloud.getCloudService()).download(fileName, fileId, path, cloud);
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

    public void deleteFile(String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        cloudManagers.get(cloud.getCloudService()).deleteFile(fileId, path, cloud);
    }

    public void renameFile(String fileName, String newName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        cloudManagers.get(cloud.getCloudService()).renameFile(fileName, fileId, newName, path, cloud);
    }

    public boolean copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest, String fileName) {
        return transferTask.copyFile(cloudSourceName, pathSource, idSource, downloadUrl, cloudDestName, pathDest, idDest, fileName);
    }

    public void moveFile(String cloudSource, String pathSource, String idSource, String downloadUrl, String cloudDest, String pathDest, String idDest, String fileName) {
        if (copyFile(cloudSource, pathSource, idSource, downloadUrl, cloudDest, pathDest, idDest, fileName)) {
            System.out.println("finished copy, start deleting");
            deleteFile(cloudSource, idSource, pathSource);
        }
    }

    public String getThumbnail(FileParameters params) {
        Cloud cloud = userManager.getCloud(params.cloudName);
        return cloudManagers.get(cloud.getCloudService()).getThumbnail(cloud, params.fileId, params.path);
    }
}