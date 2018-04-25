package com.nat.cloudman.cloud;

import com.nat.cloudman.cloud.transfer.TransferTask;
import com.nat.cloudman.controllers.params.FileParameters;
import com.nat.cloudman.controllers.params.TransitParameters;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;

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

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    private Map<String, CloudManager> cloudManagers = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(CloudManagerFacade.class);

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

    public boolean uploadFile(String cloudName, File localFile, String pathToUpload, String parentId) throws Exception {
        System.out.println("uploadFile(),");
        Cloud cloud = userManager.getCloud(cloudName);
        return cloudManagers.get(cloud.getCloudService()).uploadFile(cloud, localFile, pathToUpload, parentId);
    }

    public boolean addFolder(String folderName, String cloudName, String path, String parentId) {
        Cloud cloud = userManager.getCloud(cloudName);
        return cloudManagers.get(cloud.getCloudService()).addFolder(folderName, cloud, path, parentId);
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

    public void deleteFile(String cloudName, String fileId, String path, String parentId) {
        Cloud cloud = userManager.getCloud(cloudName);
        cloudManagers.get(cloud.getCloudService()).deleteFile(fileId, path, cloud, parentId);
    }

    public void renameFile(String fileName, String newName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        cloudManagers.get(cloud.getCloudService()).renameFile(fileName, fileId, newName, path, cloud);
    }

    public boolean copyFile(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest, String fileName, String parentId) {
        return transferTask.copyFile(cloudSourceName, pathSource, idSource, downloadUrl, cloudDestName, pathDest, idDest, fileName, parentId);
    }

    public void moveFile(TransitParameters params) {
        if (!params.cloudSource.equals(params.cloudDest)) {
            if (copyFile(params.cloudSource, params.pathSource, params.idSource, params.downloadUrl, params.cloudDest, params.pathDest, params.idDest, params.fileName, params.parentId)) {
                System.out.println("finished copy, start deleting");
                deleteFile(params.cloudSource, params.idSource, params.pathSource, params.parentId);
            }
        } else {
            Cloud cloudDest = userManager.getCloud(params.cloudDest);
            cloudManagers.get(cloudDest.getCloudService()).moveFile(params.pathSource, params.pathDest, params.idSource, params.idDest, cloudDest, params.fileName, params.parentId);
        }
    }

    public String getThumbnail(FileParameters params) {
        Cloud cloud = userManager.getCloud(params.cloudName);
        return cloudManagers.get(cloud.getCloudService()).getThumbnail(cloud, params.fileId, params.path);
    }

    public ResponseEntity<InputStreamResource> downloadFolder(String fileName, String cloudName, String fileId, String path) {
        DownloadedFileContainer fileContainer = downloadFolderFromCloud(fileName, cloudName, fileId, path);
        return Utils.fromFileContainerToResponseEntity(fileContainer);
    }

    private DownloadedFileContainer downloadFolderFromCloud(String fileName, String cloudName, String fileId, String path) {
        Cloud cloud = userManager.getCloud(cloudName);
        return cloudManagers.get(cloud.getCloudService()).downloadFolder(fileName, fileId, path, cloud);
    }

    public boolean isNeedLocalFolderCopy(TransitParameters params) {
        return false;
    }

    public boolean copyFolder(String cloudSourceName, String pathSource, String idSource, String downloadUrl, String cloudDestName, String pathDest, String idDest, String folderName, String parentId) {
        logger.debug("copyFolder");
        Cloud cloudSource = userManager.getCloud(cloudSourceName);
        Cloud cloudDest = userManager.getCloud(cloudDestName);
        String servSource = cloudSource.getCloudService();
        // Google Drive doesn't allow copy folders
        if (!cloudSourceName.equalsIgnoreCase(cloudDestName) ||
                (servSource.equalsIgnoreCase(cloudDest.getCloudService()) && servSource.equalsIgnoreCase("Google Drive"))) {
            File downloadedFolder = cloudManagers.get(cloudSource.getCloudService()).downloadFolderLocal(folderName, pathSource, "", idSource, cloudSource);
            logger.debug("downloadedFolder: " + downloadedFolder.getAbsolutePath());
            if (cloudSource.getCloudService().equalsIgnoreCase("Dropbox")) {
                //Dropbox downloads folder as zip
                String zipPath = downloadedFolder.getAbsolutePath();
                //remove .zip ext
                File unzippedFolder = new File(zipPath.substring(0, zipPath.length() - 4));
                logger.debug("unzippedFolder: " + unzippedFolder.getAbsolutePath());
                ZipUtil.unpack(downloadedFolder, unzippedFolder);
                File[] files = unzippedFolder.listFiles();
                logger.debug("files.length: " + files.length);
                if (files.length == 1) {
                    // root folder is random, contains one downloaded folder
                    downloadedFolder = files[0];
                }
                logger.debug("downloadedFolder from zip: " + downloadedFolder.getAbsolutePath());
            }
            return cloudManagers.get(cloudDest.getCloudService()).uploadFolder(cloudDest, downloadedFolder, pathDest, parentId, idDest);
        } else {
            return copyFile(cloudSourceName, pathSource, idSource, downloadUrl, cloudDestName, pathDest, idDest, folderName, parentId);
        }
    }
}