package com.nat.cloudstorage.cloud.onedrive.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nat.cloudstorage.response.DownloadedFileContainer;
import com.nat.cloudstorage.response.FilesContainer;
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
        logger.debug("OneDriveClient refreshToken: " + refreshToken + ", APP_KEY: " + config.APP_KEY
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
        logger.debug("request.getBody: " + request.getBody());
        logger.debug("request.getHeaders: " + request.getHeaders());

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String refreshToken = getResponseProperty(response, "refresh_token");
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");
            logger.debug("got refreshToken: " + refreshToken);
            logger.debug("got access_oken: " + accessToken + ", expires_in: " + expiresIn);
            return response;
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
        }
        return null;
    }

    public String requestNewAccessToken(String refreshToken) {

        logger.debug("requestNewAccessToken config.APP_KEY: " + config.APP_KEY + ", config.APP_SECRET: " + config.APP_SECRET + ", refreshToken: " + refreshToken);
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
        logger.debug("request.getBody: " + request.getBody());
        logger.debug("request.getHeaders: " + request.getHeaders());

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String gotRefreshToken = getResponseProperty(response, "refresh_token");
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");


            logger.debug("gotRefreshToken: " + gotRefreshToken);
            logger.debug("access_oken: " + accessToken + ", expires_in: " + expiresIn);
            return accessToken;
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
        }
        return null;
    }

    public FilesContainer getFilesList(String folderPath, String folderId) {

        logger.debug("oneDriveUtils. getFilesList");
        logger.debug("refreshToken: " + refreshToken);
        logger.debug("accessToken: " + accessToken);
        try {
            return getItemExpandChildrensRequest(folderPath, folderId);

        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            this.accessToken = requestNewAccessToken(refreshToken);
            return getItemExpandChildrensRequest(folderPath, folderId);
        }
    }

    public String dateConvert(String inDate) {
        logger.debug("dateConvert: " + inDate);
        try {
            DateFormat formatter;
            Date date;
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            new SimpleDateFormat();
            date = (Date) formatter.parse(inDate);
            formatter = new SimpleDateFormat("dd-MM-yyyy");
            String outDate = formatter.format(date);
            logger.debug("outDate: " + outDate);
            return outDate;
        } catch (ParseException e) {
            logger.debug("ParseException  :" + e);
        }
        return null;
    }


    public FilesContainer getItemExpandChildrensRequest(String folderPath, String folderId) {
        logger.debug("oneDriveUtils. getItemExpandChildrensRequest");
        logger.debug("folderPath: " + folderPath);
        String url;
        if (folderPath.isEmpty()) {
            url = "https://graph.microsoft.com/v1.0/me/drive/root?$expand=children";
        } else {
            url = "https://graph.microsoft.com/v1.0/me/drive/root:/" + folderPath + "?$expand=children";
        }
        logger.debug("GET url: " + url);
        logger.debug("accessToken: " + accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
        logger.debug("children get: " + getResponseProperty(response, "children"));
        String parentId = getResponseProperty(response, "id");
        logger.debug("id get: " + getResponseProperty(response, "id"));

        JsonNode valueNode = response.getBody().path("children");
        Iterator<JsonNode> iterator = valueNode.iterator();
        logger.debug("children:");

        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();

        while (iterator.hasNext()) {
            JsonNode file = iterator.next();
            logger.debug("name: " + file.get("name").asText());

            HashMap<String, String> resultFile = new HashMap<String, String>();

            if (file.has("folder")) {
                resultFile.put("type", "folder");
                logger.debug("+ " + file.get("folder").asText());
                logger.debug(")))folder: " + file.get("name").asText());
            }
            if (file.has("file")) {
                resultFile.put("type", "file");
                logger.debug("++ " + file.get("file").asText());
                logger.debug(")))file: " + file.get("name").asText());
                String downloadUrl = file.get("@microsoft.graph.downloadUrl").asText();
                logger.debug("@microsoft.graph.downloadUrl: " + downloadUrl);
                resultFile.put("downloadUrl", downloadUrl);
            }
            logger.debug("node end");
            resultFile.put("id", file.get("id").asText());
            resultFile.put("name", file.get("name").asText());
            resultFile.put("pathLower", file.get("name").asText());
            resultFile.put("modified", dateConvert(file.get("lastModifiedDateTime").asText()));
            resultFile.put("size", file.get("size").asText());
            resultFile.put("displayPath", file.get("name").asText());
            resultFile.put("parentId", file.get("parentReference").get("id").asText());
            files.add(resultFile);
        }
        logger.debug("listChildrenRequest return len: " + files.size());

        FilesContainer filesContainer = new FilesContainer(files, folderId);
        filesContainer.setParentId(parentId);
        return filesContainer;
    }

    public FilesContainer listChildrenRequest(String folderPath, String folderId) {
        logger.debug(" listChildrenRequest");

        logger.debug("folderPath: " + folderPath);
        String url;
        if (folderPath.isEmpty()) {
            url = "https://graph.microsoft.com/v1.0/me/drive/root/children";
        } else {
            url = "https://graph.microsoft.com/v1.0/me/drive/root:/" + folderPath + ":/children";
        }
        logger.debug("GET url: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
        logger.debug("value get: " + getResponseProperty(response, "value"));
        logger.debug("value path: " + getResponseProperty(response, "path"));

        JsonNode valueNode = response.getBody().path("value");
        Iterator<JsonNode> iterator = valueNode.iterator();
        logger.debug("value:");

        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();

        while (iterator.hasNext()) {
            JsonNode file = iterator.next();
            logger.debug("name: " + file.get("name").asText());

            HashMap<String, String> resultFile = new HashMap<String, String>();

            if (file.has("folder")) {
                resultFile.put("type", "folder");
                logger.debug("+ " + file.get("folder").asText());
                logger.debug(")))folder: " + file.get("name").asText());
            }
            if (file.has("file")) {
                resultFile.put("type", "file");
                logger.debug("++ " + file.get("file").asText());
                logger.debug(")))file: " + file.get("name").asText());
            }
            logger.debug("node end");
            resultFile.put("id", file.get("id").asText());
            resultFile.put("pathLower", file.get("name").asText());
            resultFile.put("modified", file.get("lastModifiedDateTime").asText());
            resultFile.put("size", file.get("size").asText());
            resultFile.put("displayPath", file.get("name").asText());
            resultFile.put("parentId", file.get("parentReference").get("id").asText());
            files.add(resultFile);
        }
        logger.debug("listChildrenRequest return len: " + files.size());
        FilesContainer filesContainer = new FilesContainer(files, folderId);
        return filesContainer;
    }

    public boolean uploadFile(File localFile, String filePath) {
        logger.debug("uploadFile, filePath: " + filePath + ", localfile: " + localFile.getAbsolutePath());
        try {
            if (localFile.length() <= CHUNKED_UPLOAD_CHUNK_SIZE) {
                return uploadSmallFile(localFile, filePath);
            } else {
                return chunkedUploadFile(localFile, filePath);
            }
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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
        logger.debug("uploadSmallFile url: " + url);
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
        logger.debug("Result - status (" + status + ") ");
        logger.debug("getBody: " + response.getBody());
        logger.debug("value get: " + getResponseProperty(response, "value"));
        logger.debug("value path: " + getResponseProperty(response, "path"));
        if (status == HttpStatus.CREATED || status == HttpStatus.OK) {
            return true;
        }
        logger.debug("failed, status: " + status);
        return false;
    }

    // TODO pause, resume, check status
    private boolean chunkedUploadFile(File localFile, String filePath) {
        logger.debug("CHUNKED_UPLOAD_CHUNK_SIZE: " + CHUNKED_UPLOAD_CHUNK_SIZE);
        Long fragmentSize = 4L << 20;
        logger.debug("fragmentSize: " + fragmentSize);
        Long fileSize = localFile.length();
        logger.debug("fileSize: " + fileSize);
        Long sentLen = 0L;

        String createSessionUrl = "https://graph.microsoft.com/v1.0/me/drive/root:/" + filePath + ":/createUploadSession";
        logger.debug("url: " + createSessionUrl);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain");
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<byte[]> entity = new HttpEntity<byte[]>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(createSessionUrl, HttpMethod.POST, entity, JsonNode.class);

        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
        String uploadUrl = getResponseProperty(response, "uploadUrl");
        String nextExpectedRanges = getResponseProperty(response, "nextExpectedRanges");
        String expirationDateTime = getResponseProperty(response, "expirationDateTime");
        logger.debug("value uploadUrl: " + uploadUrl);
        logger.debug("value nextExpectedRanges: " + nextExpectedRanges);
        logger.debug("value expirationDateTime: " + expirationDateTime);


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
        logger.debug("fileBytes Len: " + fileBytes.length);
        entity = null;
        while (sentLen < fileSize) {
            logger.debug("upload part, sentLen: " + sentLen);
            Long fileRest = fileSize - sentLen;
            Long nextPartSize = fileRest > fragmentSize ? fragmentSize : fileRest;
            logger.debug("fileRest: " + fileRest);
            logger.debug("nextPartSize: " + nextPartSize);

            restTemplate = new RestTemplate();
            headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Length", nextPartSize.toString());
            headers.set("Content-Range", "bytes " + sentLen + "-" + (sentLen + nextPartSize - 1) + "/" + fileSize);

            entity = new HttpEntity<byte[]>(Arrays.copyOfRange(fileBytes, sentLen.intValue(), (int) (sentLen + nextPartSize)), headers);
            logger.debug("entity: " + entity.toString());
            logger.debug("entity length: " + entity.getBody().length);
            try {
                response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, entity, JsonNode.class);
            } catch (Exception e) {
                logger.debug("Exception: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            logger.debug("upload next: Result - status (" + response.getStatusCode() + ") ");
            logger.debug("getBody: " + response.getBody());
            nextExpectedRanges = getResponseProperty(response, "nextExpectedRanges");
            expirationDateTime = getResponseProperty(response, "expirationDateTime");
            logger.debug("value uploadUrl: " + uploadUrl);
            logger.debug("value nextExpectedRanges: " + nextExpectedRanges);
            logger.debug("value expirationDateTime: " + expirationDateTime);

            sentLen += nextPartSize;
        }
        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.CREATED || status == HttpStatus.OK) {
            return true;
        }
        return false;
    }

    private String addFolderRequest(String folderName, String path, String parentId) {
        logger.debug("addFolderRequest, folderName: " + folderName + ", path: " + path + " parentId: " + parentId);
        String url;
        if (parentId == null || parentId.isEmpty()) {
            url = "https://graph.microsoft.com/v1.0/me/drive/root/children";
        } else {
            url = "https://graph.microsoft.com/v1.0/me/drive/items/" + parentId + "/children";
        }
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
        logger.debug(" addFolderRequest Result - status (" + status + ") ");
        logger.debug("getBody: " + response.getBody());
        if (status == HttpStatus.CREATED || status == HttpStatus.OK) {
            String folderId = getResponseProperty(response, "id");
            logger.debug("folder id: " + folderId);
            return folderId;
        }
        return null;
    }

    public String addFolder(String folderName, String path, String parentId) {
        try {
            return addFolderRequest(folderName, path, parentId);
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = requestNewAccessToken(refreshToken);
            try {
                return addFolderRequest(folderName, path, parentId);
            } catch (Exception ex) {
                logger.debug("Exception: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }
        }
    }

    public DownloadedFileContainer download(String fileName, String fileId, String path) {

        try {
            downloadRequest(fileName, fileId);
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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
        logger.debug("deleteRequest, fileName: " + filePath + ", fileId: " + fileId);
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileId;
        logger.debug("url: " + url);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
    }

    public boolean deleteFile(String fileId, String path) {
        try {
            deleteRequest(path, fileId);
            return true;
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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

    public File downloadLocal(String fileName, String downloadUrl, String downloadPath) {
        File file = new File(downloadPath + fileName);
        try {
            FileUtils.copyURLToFile(new URL(downloadUrl), file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    private void renameRequest(String newName, String fileId) {
        logger.debug("renameRequest");
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileId;
        logger.debug("url: " + url);
        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        node.put("name", newName);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        logger.debug("node.toString(): " + node.toString());
        HttpEntity<String> entity = new HttpEntity<String>(node.toString(), headers);

        //PATCH is not working for RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(requestFactory);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
    }

    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest) {

        try {
            copyRequest(pathSourse, pathDest, idSource, idDest);
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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
        logger.debug("copyRequest");
        //need to get driveId for copying
        String url = "https://graph.microsoft.com/v1.0/me/drive";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status: " + response.getStatusCode() + "getBody: " + response.getBody());
        String driveId = getResponseProperty(response, "id");
        logger.debug("driveId : " + driveId);

        //copy request
        url = "https://graph.microsoft.com/v1.0/me/drive/items/" + idSource + "/copy";
        logger.debug("copy url: " + url);
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
        logger.debug("node.toString(): " + node.toString());
        entity = new HttpEntity<String>(node.toString(), headers);

        response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
        logger.debug(" Result - status :" + response.getStatusCode() + " getBody: " + response.getBody());
    }

    private String getThumbnailRequest(String fileId) {
        logger.debug("getThumbnailRequest");
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileId + "/thumbnails?select=medium";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status: " + response.getStatusCode() + "getBody: " + response.getBody());
        JsonNode valueNode = response.getBody().path("value");
        Iterator<JsonNode> iterator = valueNode.iterator();
        while (iterator.hasNext()) {
            JsonNode thumbnailData = iterator.next();
            if (thumbnailData.has("medium")) {
                String thumbUrl = thumbnailData.path("medium").get("url").asText();
                logger.debug("url: " + url);
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
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = requestNewAccessToken(refreshToken);
            try {
                return getThumbnailRequest(fileId);
            } catch (HttpClientErrorException exc) {
                logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
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
        logger.debug("getRootFolderId");
        String url = "https://graph.microsoft.com/v1.0/me/drive/root";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status: " + response.getStatusCode() + "getBody: " + response.getBody());
        String rootId = getResponseProperty(response, "id");
        logger.debug("driveId : " + rootId);
        return rootId;
    }

    private void moveRequest(String pathSourse, String pathDest, String idSource, String idDest) {
        logger.debug("moveRequest");
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + idSource;
        logger.debug("move url: " + url);
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
        logger.debug("node.toString(): " + node.toString());
        HttpEntity<String> entity = new HttpEntity<String>(node.toString(), headers);

        //PATCH is not working for RestTemplate
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(10000);
        restTemplate.setRequestFactory(requestFactory);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
    }
}
