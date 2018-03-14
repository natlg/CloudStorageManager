package com.nat.cloudman.cloud.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ParentReference;
import com.nat.cloudman.cloud.CloudCredentials;
import com.nat.cloudman.cloud.CloudManager;
import com.nat.cloudman.cloud.google.client.GoogleClient;
import com.nat.cloudman.cloud.google.client.GoogleConfig;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.service.CloudService;
import com.nat.cloudman.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;

import java.nio.file.Files;
import java.util.*;

@Component
public class GoogleManager implements CloudManager {

    @Value("${google.app.key}")
    private String CLIENT_ID;

    @Value("${google.app.secret}")
    private String CLIENT_SECRET;

    @Value("${cloudman.domain}")
    private String APP_DOMAIN;

    @Autowired
    private CloudService cloudService;

    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    private static final Logger logger = LoggerFactory.getLogger(GoogleManager.class);

    private GoogleClient getClient(Cloud cloud) {
        if (cloud != null) {
            return new GoogleClient(new GoogleConfig(CLIENT_ID, CLIENT_SECRET, cloud.getAccessToken(), cloud.getRefreshToken()));
        }
        return new GoogleClient(new GoogleConfig(CLIENT_ID, CLIENT_SECRET, null, null));
    }

    private void checkAndSaveAccessToken(String accessToken, Cloud cloud) {
        if (!accessToken.equals(cloud.getAccessToken())) {
            cloud.setAccessToken(accessToken);
            cloudService.saveCloud(cloud);
        }
    }

    @Override
    public FilesContainer getFilesList(Cloud cloud, String folderId, String path) {
        try {
            return getClient(cloud).getFilesListRequest(folderId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
            String newAccessToken = getClient(cloud).requestNewAccessToken(cloud.getRefreshToken());
            checkAndSaveAccessToken(newAccessToken, cloud);
            try {
                return getClient(cloud).getFilesListRequest(folderId);
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public String getServiceName() {
        return "Google Drive";
    }


    private com.google.api.services.drive.model.File insertFile(Drive service, String title, String description,
                                                                String parentId, String mimeType, String filename) {
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(title);
        body.setDescription(description);
        body.setMimeType(mimeType);
        if (parentId != null && parentId.length() > 0) {
            body.setParents(
                    Arrays.asList(new ParentReference().setId(parentId)));
        }
        java.io.File fileContent = new java.io.File(filename);
        FileContent mediaContent = new FileContent(mimeType, fileContent);
        try {
            com.google.api.services.drive.model.File file = service.files().insert(body, mediaContent).execute();
            return file;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return null;
        }
    }

    private Drive getDrive(String accessToken, String refreshToken) {
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential1 = new GoogleCredential.Builder().setJsonFactory(jsonFactory)
                .setTransport(httpTransport).setClientSecrets(CLIENT_ID, CLIENT_SECRET).build();
        credential1.setAccessToken(accessToken);
        credential1.setRefreshToken(refreshToken);
        return new Drive.Builder(httpTransport, jsonFactory, credential1).build();
    }

    @Override
    public boolean uploadFile(Cloud cloud, File localFile, String pathToUpload, String parentId) {
        String fileName = Utils.getNameFromPath(pathToUpload);
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = localFile.getName();
        }
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(localFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("upload file: " + localFile.getName() + ", mime type: " + mimeType);
        if (insertFile(getDrive(cloud.getAccessToken(), cloud.getRefreshToken()),
                fileName, "", parentId, mimeType, localFile.getPath()) != null) {
            return true;
        }

        return false;
    }

    public boolean uploadFolderRecursive(final File folder, Cloud cloud, String pathToUpload, String parentId) {
        boolean result = true;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                logger.debug("folder: " + fileEntry.getAbsolutePath());
                com.google.api.services.drive.model.File addedFolder = addFolderAndGet(fileEntry.getName(), cloud, pathToUpload, parentId);
                if (addedFolder != null) {
                    logger.debug("added folder: " + addedFolder.getTitle() + ", id: " + addedFolder.getId());
                    if (!uploadFolderRecursive(fileEntry, cloud, pathToUpload + "/" + addedFolder.getTitle(), addedFolder.getId())) {
                        result = false;
                    }
                } else {
                    result = false;
                }
            } else {
                if (!uploadFile(cloud, fileEntry, pathToUpload + "/" + fileEntry.getName(), parentId)) {
                    result = false;
                }
                logger.debug("file: " + fileEntry.getAbsolutePath());
            }
        }
        logger.debug("return from google uploadFolderRecursive: " + result);
        return result;
    }

    @Override
    public boolean uploadFolder(Cloud cloud, File localFolder, String pathToUpload, String parentId) {
        logger.debug("google uploadFolder");
        // add folder first
        com.google.api.services.drive.model.File addedFolder = addFolderAndGet(localFolder.getName(), cloud, pathToUpload, parentId);
        if (addedFolder != null) {
            logger.debug("added folder: " + addedFolder.getTitle() + ", id: " + addedFolder.getId());
            return uploadFolderRecursive(localFolder, cloud, pathToUpload, addedFolder.getId());
        }
        return false;
    }

    @Override
    public boolean addFolder(String folderName, Cloud cloud, String path, String parentId) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(folderName);
        body.setMimeType("application/vnd.google-apps.folder");
        if (parentId != null && parentId.length() > 0) {
            body.setParents(
                    Arrays.asList(new ParentReference().setId(parentId)));
        }
        try {
            com.google.api.services.drive.model.File file =
                    driveService.files().insert(body).setFields("id").execute();
            if (file != null) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private com.google.api.services.drive.model.File addFolderAndGet(String folderName, Cloud cloud, String path, String parentId) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(folderName);
        body.setMimeType("application/vnd.google-apps.folder");
        if (parentId != null && parentId.length() > 0) {
            body.setParents(
                    Arrays.asList(new ParentReference().setId(parentId)));
        }
        try {
            com.google.api.services.drive.model.File addedFolder =
                    driveService.files().insert(body).setFields("id").execute();
            return addedFolder;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DownloadedFileContainer downloadFolder(String folderName, String folderId, String path, Cloud cloud) {
        String pathToDownload = DOWNLOAD_PATH + "\\" + System.currentTimeMillis() + "\\" + folderName;
        File localFolder = downloadFolderToLocalPath(folderName, path, "", folderId, cloud, pathToDownload);
        logger.debug("pathToDownload: " + pathToDownload);
        File zipFile = new File(pathToDownload + ".zip");
        ZipUtil.pack(localFolder, zipFile, true);
        return Utils.fileToContainer(zipFile, folderName + ".zip");
    }

    @Override
    public File downloadFolderLocal(String folderName, String path, String downloadUrl, String folderId, Cloud cloud) {
        logger.debug("downloadFolderLocal, folderName: " + folderName);
        String pathToDownload = DOWNLOAD_PATH + "\\" + System.currentTimeMillis() + "\\" + folderName;
        logger.debug("pathToDownload: " + pathToDownload);
        return downloadFolderToLocalPath(folderName, path, downloadUrl, folderId, cloud, pathToDownload);
    }


    private File downloadFolderToLocalPath(String folderName, String path, String downloadUrl, String folderId, Cloud cloud, String pathToDownload) {
        File localFolder = downloadFolderRecursive(path, folderId, pathToDownload, cloud);
        logger.debug("return from downloadLocalFolder: " + localFolder.getAbsolutePath());
        return localFolder;
    }

    private File downloadFolderRecursive(String path, String folderId, String pathToDownload, Cloud cloud) {
        File localFolder = new File(pathToDownload);
        //create folder
        logger.debug("downloadFolderRecursive, " + localFolder.mkdir() + localFolder.mkdirs());
        FilesContainer filesContainer = getFilesList(cloud, folderId, path);
        logger.debug("downloadFolderRecursive, pathToDownload: " + pathToDownload);
        for (HashMap<String, String> file : filesContainer.getFiles()) {
            String fileName = file.get("name");
            String fileType = file.get("type");
            String fileId = file.get("id");
            logger.debug("file: " + fileName + " " + fileType + " " + fileId);
            if (file.get("type").equalsIgnoreCase("file")) {
                //download file
                downloadToLocalPath(fileName, fileId, pathToDownload + "\\", cloud);
            } else {
                String newPath = pathToDownload + "\\" + fileName;
                //repeat
                downloadFolderRecursive(path, fileId, newPath, cloud);
            }
        }
        return localFolder;
    }

    @Override
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        File file = downloadFileLocal(fileName, path, null, fileId, cloud);
        return Utils.fileToContainer(file, fileName);
    }

    @Override
    public File downloadFileLocal(String fileName, String path, String downloadUrl, String fileId, Cloud cloud) {
        return downloadToLocalPath(fileName, fileId, DOWNLOAD_PATH + "\\" + System.currentTimeMillis(), cloud);
    }

    private File downloadToLocalPath(String fileName, String fileId, String localPath, Cloud cloud) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        //TODO timeout
        File file = new File(localPath + fileName);
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    @Override
    public boolean deleteFile(String fileId, String path, Cloud cloud, String parentId) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        System.out.println("google deleteFile, parentId: " + parentId);
        if (parentId == null || parentId.trim().isEmpty()) {
            parentId = getRootId(cloud);
        }
        try {
            driveService.files().update(fileId, null)
                    .setRemoveParents(parentId)
                    .setFields("id, parents")
                    .execute();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean renameFile(String fileName, String fileId, String newName, String path, Cloud cloud) {
        System.out.println("google renameFile");
        Drive service = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        try {
            com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();
            file.setTitle(newName);
            // Rename file.
            com.google.api.services.drive.Drive.Files.Patch patchRequest = service.files().patch(fileId, file);
            patchRequest.setFields("title");
            patchRequest.execute();
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return false;
        }
    }

    public String getRootId(Cloud cloud) {
        try {
            return getClient(cloud).getRootIdRequest(cloud.getAccessToken());
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            String newAccessToken = getClient(cloud).requestNewAccessToken(cloud.getRefreshToken());
            checkAndSaveAccessToken(newAccessToken, cloud);
            try {
                return getClient(cloud).getRootIdRequest(cloud.getAccessToken());
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud, String fileName, String parentId) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        System.out.println("google copyFile, idDest: " + idDest);
        com.google.api.services.drive.model.File file = null;
        if (idDest == null || idDest.trim().isEmpty()) {
            idDest = getRootId(cloud);
        }
        com.google.api.services.drive.model.File copiedFile = new com.google.api.services.drive.model.File();
        copiedFile.setTitle(fileName);
        try {
            file = driveService.files().copy(idSource, copiedFile).execute();
            String idCopied = file.getId();
            file = driveService.files().update(idCopied, null)
                    .setAddParents(idDest)
                    .setFields("id, parents")
                    .execute();
            file = driveService.files().update(idCopied, null)
                    .setRemoveParents(parentId)
                    .setFields("id, parents")
                    .execute();
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return false;
        }
    }

    @Override
    public boolean moveFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud, String fileName, String parentId) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        System.out.println("google moveFile, idDest: " + idDest);
        if (idDest == null || idDest.trim().isEmpty()) {
            idDest = getRootId(cloud);
        }
        try {
            driveService.files().update(idSource, null)
                    .setAddParents(idDest)
                    .setFields("id, parents")
                    .execute();
            driveService.files().update(idSource, null)
                    .setRemoveParents(parentId)
                    .setFields("id, parents")
                    .execute();
            return true;
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return false;
        }
    }

    @Override
    public String getThumbnail(Cloud cloud, String fileId, String path) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        com.google.api.services.drive.model.File file = null;
        try {
            file = driveService.files().get(fileId)
                    .execute();
            String thumb = file.getThumbnailLink();
            System.out.println("Thumbnail: " + thumb);
            return thumb;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CloudCredentials sendAuthorizationCodeRequest(String code) {
        return getClient(null).sendAuthorizationCodeRequest(code, APP_DOMAIN);
    }
}
