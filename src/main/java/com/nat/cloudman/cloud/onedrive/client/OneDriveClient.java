package com.nat.cloudman.cloud.onedrive.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OneDriveClient {

    private static final Logger logger = LoggerFactory.getLogger(OneDriveClient.class);

    private String accessToken;
    private String refreshToken;

    private OneDriveConfig config;

    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 4L << 20; // 4MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    public OneDriveClient(OneDriveConfig config, String accessToken, String refreshToken) {
        this.config = config;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        System.out.println("OneDriveClient refreshToken: " + refreshToken + ", APP_KEY: " + config.APP_KEY
                + ", APP_SECRET: " + config.APP_SECRET + ", accessToken: " + accessToken);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public ResponseEntity<JsonNode> sendAuthorizationCodeRequest(String code) {
        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", config.APP_KEY);
        map.add("scope", "Files.ReadWrite.All offline_access");
        map.add("code", code);
        map.add("redirect_uri", "http://localhost:8080/index.html");
        map.add("grant_type", "authorization_code");
        map.add("client_secret", config.APP_SECRET);

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
            return response;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
        }
        return null;
    }

    public String requestNewAccessToken(String refreshToken) {

        System.out.println("requestNewAccessToken config.APP_KEY: " + config.APP_KEY + ", config.APP_SECRET: " + config.APP_SECRET + ", refreshToken: " + refreshToken);
        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token ";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", config.APP_KEY);
        map.add("scope", "Files.ReadWrite.All offline_access");
        map.add("refresh_token", refreshToken);
        map.add("redirect_uri", "http://localhost:8080/index.html");
        map.add("grant_type", "refresh_token");
        map.add("client_secret", config.APP_SECRET);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        System.out.println("request.getBody: " + request.getBody());
        System.out.println("request.getHeaders: " + request.getHeaders());

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String gotRefreshToken = getResponseProperty(response, "refresh_token");
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");


            System.out.println("gotRefreshToken: " + gotRefreshToken);
            System.out.println("access_oken: " + accessToken + ", expires_in: " + expiresIn);
            return accessToken;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
        }
        return null;
    }

    public FilesContainer getFilesList(String folderPath, String folderId) {

        System.out.println("oneDriveUtils. getFilesList");
        System.out.println("refreshToken: " + refreshToken);
        System.out.println("accessToken: " + accessToken);
        try {
            return getItemExpandChildrensRequest(folderPath, folderId);

        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            this.accessToken = requestNewAccessToken(refreshToken);
            return getItemExpandChildrensRequest(folderPath, folderId);
        }
    }

    public String dateConvert(String inDate) {
        System.out.println("dateConvert: " + inDate);
        try {
            DateFormat formatter;
            Date date;
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            new SimpleDateFormat();
            date = (Date) formatter.parse(inDate);
            formatter = new SimpleDateFormat("dd-MM-yyyy");
            String outDate = formatter.format(date);
            System.out.println("outDate: " + outDate);
            return outDate;
        } catch (ParseException e) {
            System.out.println("ParseException  :" + e);
        }
        return null;
    }


    public FilesContainer getItemExpandChildrensRequest(String folderPath, String folderId) {
        System.out.println("oneDriveUtils. getItemExpandChildrensRequest");
        System.out.println("folderPath: " + folderPath);
        String url;
        if (folderPath.isEmpty()) {
            url = "https://graph.microsoft.com/v1.0/me/drive/root?$expand=children";
        } else {
            url = "https://graph.microsoft.com/v1.0/me/drive/root:/" + folderPath + "?$expand=children";
        }
        System.out.println("GET url: " + url);
        System.out.println("accessToken: " + accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
        System.out.println("children get: " + getResponseProperty(response, "children"));
        String parentId = getResponseProperty(response, "id");
        System.out.println("id get: " + getResponseProperty(response, "id"));

        JsonNode valueNode = response.getBody().path("children");
        Iterator<JsonNode> iterator = valueNode.iterator();
        System.out.println("children:");

        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();

        while (iterator.hasNext()) {
            JsonNode file = iterator.next();
            System.out.println("name: " + file.get("name").asText());

            HashMap<String, String> resultFile = new HashMap<String, String>();

            if (file.has("folder")) {
                resultFile.put("type", "folder");
                System.out.println("+ " + file.get("folder").asText());
                System.out.println(")))folder: " + file.get("name").asText());
            }
            if (file.has("file")) {
                resultFile.put("type", "file");
                System.out.println("++ " + file.get("file").asText());
                System.out.println(")))file: " + file.get("name").asText());
                String downloadUrl = file.get("@microsoft.graph.downloadUrl").asText();
                System.out.println("@microsoft.graph.downloadUrl: " + downloadUrl);
                resultFile.put("downloadUrl", downloadUrl);
            }
            System.out.println("node end");
            resultFile.put("id", file.get("id").asText());
            resultFile.put("name", file.get("name").asText());
            resultFile.put("pathLower", file.get("name").asText());
            resultFile.put("modified", dateConvert(file.get("lastModifiedDateTime").asText()));
            resultFile.put("size", file.get("size").asText());
            resultFile.put("displayPath", file.get("name").asText());
            resultFile.put("parentId", file.get("parentReference").get("id").asText());
            files.add(resultFile);
        }
        System.out.println("listChildrenRequest return len: " + files.size());

        FilesContainer filesContainer = new FilesContainer(files, folderId);
        filesContainer.setParentId(parentId);
        return filesContainer;
    }

    public FilesContainer listChildrenRequest(String folderPath, String folderId) {
        System.out.println(" listChildrenRequest");

        System.out.println("folderPath: " + folderPath);
        String url;
        if (folderPath.isEmpty()) {
            url = "https://graph.microsoft.com/v1.0/me/drive/root/children";
        } else {
            url = "https://graph.microsoft.com/v1.0/me/drive/root:/" + folderPath + ":/children";
        }
        System.out.println("GET url: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
        System.out.println("value get: " + getResponseProperty(response, "value"));
        System.out.println("value path: " + getResponseProperty(response, "path"));

        JsonNode valueNode = response.getBody().path("value");
        Iterator<JsonNode> iterator = valueNode.iterator();
        System.out.println("value:");

        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();

        while (iterator.hasNext()) {
            JsonNode file = iterator.next();
            System.out.println("name: " + file.get("name").asText());

            HashMap<String, String> resultFile = new HashMap<String, String>();

            if (file.has("folder")) {
                resultFile.put("type", "folder");
                System.out.println("+ " + file.get("folder").asText());
                System.out.println(")))folder: " + file.get("name").asText());
            }
            if (file.has("file")) {
                resultFile.put("type", "file");
                System.out.println("++ " + file.get("file").asText());
                System.out.println(")))file: " + file.get("name").asText());
            }
            System.out.println("node end");
            resultFile.put("id", file.get("id").asText());
            resultFile.put("pathLower", file.get("name").asText());
            resultFile.put("modified", file.get("lastModifiedDateTime").asText());
            resultFile.put("size", file.get("size").asText());
            resultFile.put("displayPath", file.get("name").asText());
            resultFile.put("parentId", file.get("parentReference").get("id").asText());
            files.add(resultFile);
        }
        System.out.println("listChildrenRequest return len: " + files.size());
        FilesContainer filesContainer = new FilesContainer(files, folderId);
        return filesContainer;
    }

    public boolean uploadFile(File localFile, String filePath) {
        System.out.println("uploadFile" + "filePath: " + filePath);
        try {
            if (localFile.length() <= CHUNKED_UPLOAD_CHUNK_SIZE) {
                return uploadSmallFile(localFile, filePath);
            } else {
                return chunkedUploadFile(localFile, filePath);
            }
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            if (localFile.length() <= CHUNKED_UPLOAD_CHUNK_SIZE) {
                return uploadSmallFile(localFile, filePath);
            } else {
                return chunkedUploadFile(localFile, filePath);
            }
        }
    }

    private boolean uploadSmallFile(File localFile, String filePath) {
        String url = "https://graph.microsoft.com/v1.0/me/drive/root:/" + filePath + ":/content";
        System.out.println("url: " + url);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        InputStream in = null;
        try {
            in = new FileInputStream(localFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        HttpEntity<byte[]> entity = null;
        try {
            entity = new HttpEntity<byte[]>(IOUtils.toByteArray(in), headers);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PUT, entity, JsonNode.class);
        HttpStatus status = response.getStatusCode();
        System.out.println("Result - status (" + status + ") ");
        System.out.println("getBody: " + response.getBody());
        System.out.println("value get: " + getResponseProperty(response, "value"));
        System.out.println("value path: " + getResponseProperty(response, "path"));
        if (status == HttpStatus.CREATED || status == HttpStatus.OK) {
            return true;
        }
        return false;
    }

    // TODO pause, resume, check status
    private boolean chunkedUploadFile(File localFile, String filePath) {
        System.out.println("CHUNKED_UPLOAD_CHUNK_SIZE: " + CHUNKED_UPLOAD_CHUNK_SIZE);
        Long fragmentSize = 4L << 20;
        System.out.println("fragmentSize: " + fragmentSize);
        Long fileSize = localFile.length();
        System.out.println("fileSize: " + fileSize);
        Long sentLen = 0L;

        String createSessionUrl = "https://graph.microsoft.com/v1.0/me/drive/root:/" + filePath + ":/createUploadSession";
        System.out.println("url: " + createSessionUrl);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain");
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<byte[]> entity = new HttpEntity<byte[]>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(createSessionUrl, HttpMethod.POST, entity, JsonNode.class);

        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
        String uploadUrl = getResponseProperty(response, "uploadUrl");
        String nextExpectedRanges = getResponseProperty(response, "nextExpectedRanges");
        String expirationDateTime = getResponseProperty(response, "expirationDateTime");
        System.out.println("value uploadUrl: " + uploadUrl);
        System.out.println("value nextExpectedRanges: " + nextExpectedRanges);
        System.out.println("value expirationDateTime: " + expirationDateTime);


        //////////////////////////////////////////// upload

        InputStream in = null;
        try {
            in = new FileInputStream(localFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] fileBytes = null;
        try {
            fileBytes = IOUtils.toByteArray(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("fileBytes Len: " + fileBytes.length);
        entity = null;
        while (sentLen < fileSize) {
            System.out.println("upload part, sentLen: " + sentLen);
            Long fileRest = fileSize - sentLen;
            Long nextPartSize = fileRest > fragmentSize ? fragmentSize : fileRest;
            System.out.println("fileRest: " + fileRest);
            System.out.println("nextPartSize: " + nextPartSize);

            restTemplate = new RestTemplate();
            headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Length", nextPartSize.toString());
            headers.set("Content-Range", "bytes " + sentLen + "-" + (sentLen + nextPartSize - 1) + "/" + fileSize);

            entity = new HttpEntity<byte[]>(Arrays.copyOfRange(fileBytes, sentLen.intValue(), (int) (sentLen + nextPartSize)), headers);
            System.out.println("entity: " + entity.toString());
            System.out.println("entity length: " + entity.getBody().length);
            try {
                response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, entity, JsonNode.class);
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            System.out.println("upload next: Result - status (" + response.getStatusCode() + ") ");
            System.out.println("getBody: " + response.getBody());
            nextExpectedRanges = getResponseProperty(response, "nextExpectedRanges");
            expirationDateTime = getResponseProperty(response, "expirationDateTime");
            System.out.println("value uploadUrl: " + uploadUrl);
            System.out.println("value nextExpectedRanges: " + nextExpectedRanges);
            System.out.println("value expirationDateTime: " + expirationDateTime);

            sentLen += nextPartSize;
        }
        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.CREATED || status == HttpStatus.OK) {
            return true;
        }
        return false;
    }

    private boolean addFolderRequest(String folderName, String path, String parentId) {
        logger.debug("addFolderRequest, folderName: " + folderName + ", path: " + path + " parentId: " + parentId);
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + parentId + "/children";
        logger.debug("url: " + url);

        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode folderNode = nodeFactory.objectNode();

        ObjectNode child = nodeFactory.objectNode(); // the child

        child.put("childCount", 0);

        folderNode.set("folder", child);
        folderNode.put("name", folderName);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        logger.debug("folderNode.toString(): " + folderNode.toString());

        HttpEntity<String> entity = new HttpEntity<String>(folderNode.toString(), headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        HttpStatus status = response.getStatusCode();
        logger.debug("Result - status (" + status + ") ");
        logger.debug("getBody: " + response.getBody());
        if (status == HttpStatus.CREATED || status == HttpStatus.OK) {
            return true;
        }
        return false;
    }

    public boolean addFolder(String folderName, String path, String parentId) {
        try {
            return addFolderRequest(folderName, path, parentId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = requestNewAccessToken(refreshToken);
            try {
                return addFolderRequest(folderName, path, parentId);
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public DownloadedFileContainer download(String fileName, String fileId, String path) {

        try {
            downloadRequest(fileName, fileId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = requestNewAccessToken(refreshToken);
            downloadRequest(fileName, fileId);
        }
        return null;
    }

    //TODO
    private void downloadRequest(String fileName, String fileId) {

    }

    public void deleteRequest(String filePath, String fileId) {
        System.out.println("deleteRequest, fileName: " + filePath + ", fileId: " + fileId);
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileId;
        System.out.println("url: " + url);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
    }

    public boolean deleteFile(String fileId, String path) {
        try {
            deleteRequest(path, fileId);
            return true;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            try {
                deleteRequest(path, fileId);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public boolean renameFile(String fileName, String fileId, String newName, String path) {
        try {
            renameRequest(newName, fileId);
            return true;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            try {
                renameRequest(newName, fileId);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    private String getResponseProperty(ResponseEntity<JsonNode> response, String property) {
        String value = null;
        JsonNode node = response.getBody().get(property);
        if (node != null) {
            value = node.asText();
        }
        return value;
    }

    public File downloadLocal(String fileName, String path, String downloadUrl, String downloadPath) {
        File file = new File(downloadPath + System.currentTimeMillis() + fileName);
        try {
            FileUtils.copyURLToFile(new URL(downloadUrl), file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    private void renameRequest(String newName, String fileId) {
        System.out.println("renameRequest");
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileId;
        System.out.println("url: " + url);
        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        node.put("name", newName);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        System.out.println("node.toString(): " + node.toString());
        HttpEntity<String> entity = new HttpEntity<String>(node.toString(), headers);

        //PATCH is not working for RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(requestFactory);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
    }

    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest) {

        try {
            copyRequest(pathSourse, pathDest, idSource, idDest);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            try {
                copyRequest(pathSourse, pathDest, idSource, idDest);
            } catch (HttpClientErrorException exc) {
                return false;
            }
        }
        return true;
    }

    private void copyRequest(String pathSourse, String pathDest, String idSource, String idDest) {
        //TODO make boolean, check status
        System.out.println("copyRequest");
        //need to get driveId for copying
        String url = "https://graph.microsoft.com/v1.0/me/drive";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status: " + response.getStatusCode() + "getBody: " + response.getBody());
        String driveId = getResponseProperty(response, "id");
        System.out.println("driveId : " + driveId);

        //copy request
        url = "https://graph.microsoft.com/v1.0/me/drive/items/" + idSource + "/copy";
        System.out.println("copy url: " + url);
        //destination information
        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        ObjectNode parentReferenceNode = nodeFactory.objectNode();
        if (idDest == null || idDest.isEmpty()) {
            idDest = getRootFolderId();
        }
        parentReferenceNode.put("driveId", driveId);
        parentReferenceNode.put("id", idDest);
        node.set("parentReference", parentReferenceNode);
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        System.out.println("node.toString(): " + node.toString());
        entity = new HttpEntity<String>(node.toString(), headers);

        response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
        System.out.println(" Result - status :" + response.getStatusCode() + " getBody: " + response.getBody());
    }

    private String getThumbnailRequest(String fileId) {
        System.out.println("getThumbnailRequest");
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileId + "/thumbnails?select=medium";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status: " + response.getStatusCode() + "getBody: " + response.getBody());
        JsonNode valueNode = response.getBody().path("value");
        Iterator<JsonNode> iterator = valueNode.iterator();
        while (iterator.hasNext()) {
            JsonNode thumbnailData = iterator.next();
            if (thumbnailData.has("medium")) {
                String thumbUrl = thumbnailData.path("medium").get("url").asText();
                System.out.println("url: " + url);
                //we need only one size
                return thumbUrl;
            }
        }
        return null;
    }

    public String getThumbnail(String fileId) {
        try {
            return getThumbnailRequest(fileId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            try {
                return getThumbnailRequest(fileId);
            } catch (HttpClientErrorException exc) {
                System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                        + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                        + " getStackTrace: " + e.getStackTrace());
                return null;
            }
        }
    }

    public boolean moveFile(String pathSourse, String pathDest, String idSource, String idDest) {
        try {
            moveRequest(pathSourse, pathDest, idSource, idDest);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            try {
                moveRequest(pathSourse, pathDest, idSource, idDest);
            } catch (HttpClientErrorException exc) {
                return false;
            }
        }
        return true;
    }

    private String getRootFolderId() {
        System.out.println("getRootFolderId");
        String url = "https://graph.microsoft.com/v1.0/me/drive/root";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        System.out.println("Result - status: " + response.getStatusCode() + "getBody: " + response.getBody());
        String rootId = getResponseProperty(response, "id");
        System.out.println("driveId : " + rootId);
        return rootId;
    }

    private void moveRequest(String pathSourse, String pathDest, String idSource, String idDest) {
        System.out.println("moveRequest");
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + idSource;
        System.out.println("move url: " + url);
        //destination information
        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        ObjectNode parentReferenceNode = nodeFactory.objectNode();
        if (idDest == null || idDest.isEmpty()) {
            idDest = getRootFolderId();
        }
        parentReferenceNode.put("id", idDest);
        node.set("parentReference", parentReferenceNode);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        System.out.println("node.toString(): " + node.toString());
        HttpEntity<String> entity = new HttpEntity<String>(node.toString(), headers);

        //PATCH is not working for RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(requestFactory);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
    }
}
