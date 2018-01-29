package com.nat.cloudman.cloud.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.nat.cloudman.cloud.CloudCredentials;
import com.nat.cloudman.cloud.CloudManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.service.CloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

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

    private String requestNewAccessToken(String refreshToken) {
        String url = "https://www.googleapis.com/oauth2/v4/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", CLIENT_ID);
        map.add("client_secret", CLIENT_SECRET);
        map.add("refresh_token", refreshToken);
        map.add("grant_type", "refresh_token");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        System.out.println("request.getBody: " + request.getBody());
        System.out.println("request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");
            System.out.println("got access_oken: " + accessToken + ", expires_in: " + expiresIn);
            return accessToken;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
        }
        return null;
    }

    private void checkAndSaveAccessToken(String accessToken, Cloud cloud) {
        if (!accessToken.equals(cloud.getAccessToken())) {
            cloud.setAccessToken(accessToken);
            cloudService.saveCloud(cloud);
        }
    }

    private String getRootIdRequest(String accessToken) {
        String url = "https://www.googleapis.com/drive/v3/files/" + "root";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        System.out.println("request.getBody: " + request.getBody());
        System.out.println("request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
        String rootId = getResponseProperty(response, "id");
        System.out.println("id : " + rootId);
        return rootId;
    }

    private String getRootId(Cloud cloud) {
        try {
            return getRootIdRequest(cloud.getAccessToken());
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
            String newAccessToken = requestNewAccessToken(cloud.getRefreshToken());
            checkAndSaveAccessToken(newAccessToken, cloud);
            try {
                return getRootIdRequest(newAccessToken);
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    private FilesContainer getFilesListRequest(Cloud cloud, String folderId) {
        if (folderId == null || folderId.isEmpty()) {
            folderId = getRootId(cloud);
        }
        String url = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "' in parents";
        System.out.println("url: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + cloud.getAccessToken());
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        System.out.println("request.getBody: " + request.getBody());
        System.out.println("request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
        System.out.println("files: " + getResponseProperty(response, "files"));
        JsonNode valueNode = response.getBody().path("files");
        Iterator<JsonNode> iterator = valueNode.iterator();
        System.out.println("files:");
        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();
        while (iterator.hasNext()) {
            JsonNode file = iterator.next();
            System.out.println("name: " + file.get("name").asText());
            HashMap<String, String> resultFile = new HashMap<String, String>();
            if (file.get("mimeType").asText().equals("application/vnd.google-apps.folder")) {
                resultFile.put("type", "folder");
                System.out.println("folder: " + file.get("name").asText());
            } else {
                resultFile.put("type", "file");
                System.out.println("file: " + file.get("name").asText());
            }
            resultFile.put("name", file.get("name").asText());
            resultFile.put("id", file.get("id").asText());
            files.add(resultFile);
        }
        System.out.println("listChildrenRequest return len: " + files.size());
        return new FilesContainer(files, folderId);
    }

    @Override
    public FilesContainer getFilesList(Cloud cloud, String folderId, String path) {
        try {
            return getFilesListRequest(cloud, folderId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
            String newAccessToken = requestNewAccessToken(cloud.getRefreshToken());
            checkAndSaveAccessToken(newAccessToken, cloud);
            //cloud.setAccessToken(newAccessToken);
            try {
                return getFilesListRequest(cloud, folderId);
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

    @Override
    public boolean uploadFile(Cloud cloud, File localFile, String pathToUpload) {
        return false;
    }

    @Override
    public boolean addFolder(String folderName, Cloud cloud, String path, String parentId) {
        return false;
    }

    @Override
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        return null;
    }

    @Override
    public boolean deleteFile(String fileId, String path, Cloud cloud) {
        return false;
    }

    @Override
    public boolean renameFile(String fileName, String fileId, String newName, String path, Cloud cloud) {
        return false;
    }

    @Override
    public File downloadLocal(String fileName, String path, String downloadUrl, Cloud cloud) {
        return null;
    }

    @Override
    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud) {
        return false;
    }

    @Override
    public String getThumbnail(Cloud cloud, String fileId, String path) {
        return null;
    }

    public CloudCredentials sendAuthorizationCodeRequest(String code) {
        String url = "https://www.googleapis.com/oauth2/v4/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", CLIENT_ID);
        map.add("code", code);
        map.add("redirect_uri", APP_DOMAIN + "/indexpage.html");
        map.add("grant_type", "authorization_code");
        map.add("client_secret", CLIENT_SECRET);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        System.out.println("request.getBody: " + request.getBody());
        System.out.println("request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String refreshToken = getResponseProperty(response, "refresh_token");
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");
            System.out.println("got refreshToken: " + refreshToken);
            System.out.println("got access_oken: " + accessToken + ", expires_in: " + expiresIn);
            return new CloudCredentials(accessToken, refreshToken);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
        }
        return null;
    }

    private String getResponseProperty(ResponseEntity<JsonNode> response, String property) {
        String value = null;
        JsonNode node = response.getBody().get(property);
        if (node != null) {
            value = node.asText();
        }
        return value;
    }
}
