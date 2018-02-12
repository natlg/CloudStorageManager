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
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

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

    private GoogleClient getClient(Cloud cloud) {
        return new GoogleClient(new GoogleConfig(CLIENT_ID, CLIENT_SECRET, cloud.getAccessToken(), cloud.getRefreshToken()));
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
        insertFile(getDrive(cloud.getAccessToken(), cloud.getRefreshToken()),
                fileName, "", parentId, mimeType, localFile.getPath());
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
            driveService.files().insert(body).setFields("id").execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public File downloadLocal(String fileName, String path, String downloadUrl, String fileId, Cloud cloud) {
        Drive driveService = getDrive(cloud.getAccessToken(), cloud.getRefreshToken());
        //TODO timeout
        File file = new File(DOWNLOAD_PATH + System.currentTimeMillis() + fileName);
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
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        File file = downloadLocal(fileName, path, null, fileId, cloud);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("downloaded ");
        try {
            byte[] arr = IOUtils.toByteArray(is);
            is.close();
            if (file.delete()) {
                System.out.println(file.getName() + " is deleted");
            } else {
                System.out.println("Delete operation is failed");
            }
            return new DownloadedFileContainer(fileName, arr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
