package com.nat.cloudman.cloud;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

@Component
public class OneDriveManager implements CloudManager {

    @Value("${onedrive.app.key}")
    private String APP_KEY;

    @Value("${onedrive.app.secret}")
    private String APP_SECRET;

    private String accessToken;
    private String refreshToken;

    @Autowired
    private UserManager userManager;

    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 4L << 20; // 4MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;


    @Value("${temp.download.path}")
    private String DOWNLOAD_PATH;

    @Override
    public String getServiceName() {
        return "OneDrive";
    }

    public ResponseEntity<JsonNode> sendAuthorizationCodeRequest(String code) {
        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", APP_KEY);
        map.add("scope", "Files.ReadWrite.All offline_access");
        map.add("code", code);
        map.add("redirect_uri", "http://localhost:8080/indexpage.html");
        map.add("grant_type", "authorization_code");
        map.add("client_secret", APP_SECRET);

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


    public String getRefreshToken(String code) {

        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", APP_KEY);
        map.add("scope", "Files.ReadWrite.All offline_access");
        map.add("code", code);
        map.add("redirect_uri", "http://localhost:8080/indexpage.html");
        map.add("grant_type", "authorization_code");
        map.add("client_secret", APP_SECRET);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        System.out.println("request.getBody: " + request.getBody());
        System.out.println("request.getHeaders: " + request.getHeaders());

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String refreshToken = getResponseProperty(response, "refresh_token");
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");
            System.out.println("got token: " + refreshToken);
            System.out.println("got access_oken: " + accessToken + " expires_in: " + expiresIn);
            return refreshToken;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
        }
        return null;
    }


    public String getAccessToken(String refreshToken) {

        System.out.println("getAccessToken");
        String url = "https://login.microsoftonline.com/common/oauth2/v2.0/token ";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", APP_KEY);
        map.add("scope", "Files.ReadWrite.All offline_access");
        map.add("refresh_token", refreshToken);
        map.add("redirect_uri", "http://localhost:8080/indexpage.html");
        map.add("grant_type", "refresh_token");
        map.add("client_secret", APP_SECRET);

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

    @Override
    public FilesContainer getFilesList(String accountName, String folderPath) {
        Cloud cloud = userManager.getCloud(accountName);
        String accessToken = cloud.getAccessToken();
        String refreshToken = cloud.getRefreshToken();
        setAccessToken(accessToken);
        setRefreshToken(refreshToken);
        System.out.println("oneDriveUtils. getFilesList");
        System.out.println("refreshToken: " + refreshToken);
        System.out.println("accessToken: " + accessToken);
        try {
            return getItemExpandChildrensRequest(folderPath);

        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            return getItemExpandChildrensRequest(folderPath);
        }
    }

    //TODO
    private void downloadRequest(String fileName, String fileId) {

    }

    public FilesContainer getItemExpandChildrensRequest(String folderPath) {
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
            resultFile.put("pathLower", file.get("name").asText());
            resultFile.put("modified", file.get("lastModifiedDateTime").asText());
            resultFile.put("size", file.get("size").asText());
            resultFile.put("displayPath", file.get("name").asText());
            resultFile.put("parentId", file.get("parentReference").get("id").asText());
            files.add(resultFile);
        }
        System.out.println("listChildrenRequest return len: " + files.size());

        FilesContainer filesContainer = new FilesContainer(files);
        filesContainer.setParentId(parentId);
        return filesContainer;
    }

    public FilesContainer listChildrenRequest(String folderPath) {
        System.out.println("oneDriveUtils. listChildrenRequest");

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
        FilesContainer filesContainer = new FilesContainer(files);
        return filesContainer;
    }

    public static void main(String[] args) {
        String accessToken = "EwBAA8l6BAAU7p9QDpi/D7xJLwsTgCg3TskyTaQAAalrhYbyUHh8z63NibrtFPGDLiKdBWTPE3QC9lVRUnvqNwuJyl0qjVJrhNpTeP62EP5sHPOSl7J15UMbywOny9jEq7vO1+xfc/Vezy5xAKUkyjYtloeDesvsLteUvZtxsp+DGGjWkvansXy6x3Eav3+ZoeWd7kbu7FwAEJmMZaF0ASeGQPfjG09JzBm7Ilj0TzMg2iBwaK6RfCQBGqPB4TQDnUFapkYCPKbfnbJQ12sdWvdXNxpCywPdwTg7POA1ISSW0nBQ3YRu9P13VpQRkS9qywRQwIAvEnW3Dd467taYyzaeMcxyWNjG1HkJOlHh/8yAX90ApAbtvieekd/8LpcDZgAACBDuJn9ZRFynEAINNqy1QX0nGJ4Jedq0eRh6pdyXDveU4HE+nJU0TYRY1v99S2VzA8Fg0B3Z6XWwjmMuIVDX5wCOuZ2JkbNHAoGA8JXzhCvzjrwabg5Y0zKMy5+8S8KUV/dTtR/A3J5WESpgTsXfsGmvtCx2VGSoz/sqagJTaprwFVvy+Sswz7KgZAEtTJg1q8HDsxJbpuRpfJZ3Yvm/l32Ch1NXKhs6q16NL4BDIi5Gqo/YPPrHRw+kGnLkug3QE6Pvkeiys1jRsNE4vTV3/J3iquDEOQ3bqLtMqtFMpvKwivQ9e/LOmltCt8vcy3Kayb3o5ZZzr2FtRV7ka2vt9PCBbFhWRAosNcZ1aGMkNwX98QnOMrz6KUJYJf5NOjEPU3LTHO8oKHrUtKv0NuPU6Zq8FiKaGNLl1cvInfDY3bbvTt+CriHFpQD/XlV/e/+pBB224HB8prqxkeexYGKY92rHnwPjMGBdidbUpQ5MIEvSlNXZFsmtJJTATcUBMe85xycm3EgrRSu40HOYG8rcAwM4+DW9d1OEmpOF2D9QQgAS0HPo8/hiDyRmKzNco8/JHCWduYsisd0Hdntc8/3lDx9g3SL7MxQyjbA1Xgp8eOFsyOcMRpQ9BM9F9BrEnzfbtkdoRnu05OVe/Y1qdesSp0hpmwcVZ4bIEFSttOrGheVanKKezlhSzuOzTdhkBJ4Wnz0fGw226dURY8hEAg==";
        String refreshToken = "MCVncBlg6Frfu2G3tB9!kmnveR1PEO2BcXpxx37uwT4!cX68nW3CJdUDD!Q91ZSVAv035DZmidb92rLP609WArg2BISBAFID!qICI1j!aWYFeB94tuZtDjz4BGWiOMsjiL6*8JFsNY93CsPlRqcEKcdls!H4HMSakKdZiEgPJRbmPZBpt0yKkr3fm6Kg5QDDDB0VQ9C*z3TcWs2mI6ebvveJ7a!FE6DuDgXuj1FZvQcNiZehaDAn4ICz9bv7aQn*7iz9xTX3V7PrcIu5wS63YlIeMIE*tz4EsAF3aw*4oPQHaipPzMYjCTp0UHf3Hd8g*oqec9W*XVmLpIlBlu6xuDQxTfSVJd9iKMziNEh4mgazJ";
        OneDriveManager oneDrive = new OneDriveManager();
        oneDrive.APP_KEY = "70a0893e-f51c-4f4b-abc0-827f347e4f43";
        oneDrive.APP_SECRET = "RHWfzUvTDnN5DuHbkWsewmx";
        oneDrive.setRefreshToken(refreshToken);
        oneDrive.setAccessToken(accessToken);
        try {
            oneDrive.listChildrenRequest("");
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = oneDrive.getAccessToken(refreshToken);
            oneDrive.setAccessToken(accessToken);
            oneDrive.listChildrenRequest("");
        }

    }

    //TODO remove
    public void setRefreshToken(String refreshToken) {
        System.out.println("refreshToken before: " + this.refreshToken);
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public boolean uploadFile(Cloud cloud, File localFile, String filePath) {
        //TODO check status, return bool
        System.err.println("uploadFile" + "filePath: " + filePath);
        setRefreshToken(cloud.getRefreshToken());
        try {
            if (localFile.length() <= CHUNKED_UPLOAD_CHUNK_SIZE) {
                uploadSmallFile(localFile, filePath);
            } else {
                chunkedUploadFile(localFile, filePath);
            }

        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            if (localFile.length() <= CHUNKED_UPLOAD_CHUNK_SIZE) {
                uploadSmallFile(localFile, filePath);
            } else {
                chunkedUploadFile(localFile, filePath);
            }
        }
        return true;
    }

    private void uploadSmallFile(File localFile, String filePath) {
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
        }
        HttpEntity<byte[]> entity = null;
        try {
            entity = new HttpEntity<byte[]>(IOUtils.toByteArray(in), headers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PUT, entity, JsonNode.class);
        System.out.println("Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
        System.out.println("value get: " + getResponseProperty(response, "value"));
        System.out.println("value path: " + getResponseProperty(response, "path"));
    }

    // TODO pause, resume, check status
    private void chunkedUploadFile(File localFile, String filePath) {
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
                return;
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
    }

    private String getResponseProperty(ResponseEntity<JsonNode> response, String property) {
        String value = null;
        JsonNode node = response.getBody().get(property);
        if (node != null) {
            value = node.asText();
        }
        return value;
    }

    private void addFolder(String folderName, String path, String parentId) {
        System.out.println("addFolder, folderName: " + folderName + ", path: " + path + " parentId: " + parentId);
        String url = "https://graph.microsoft.com/v1.0/me/drive/items/" + parentId + "/children";
        System.out.println("url: " + url);

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

        System.out.println("folderNode.toString(): " + folderNode.toString());

        HttpEntity<String> entity = new HttpEntity<String>(folderNode.toString(), headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);

        System.out.println("upload next: Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
    }

    @Override
    public void addFolder(String folderName, Cloud cloud, String path, String parentId) {
        setRefreshToken(cloud.getRefreshToken());
        try {
            addFolder(folderName, path, parentId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            addFolder(folderName, path, parentId);
        }
    }

    @Override
    public DownloadedFileContainer download(String fileName, String fileId, String path, Cloud cloud) {
        setRefreshToken(cloud.getRefreshToken());
        try {
            downloadRequest(fileName, fileId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            downloadRequest(fileName, fileId);
        }
        return null;
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

    @Override
    public void deleteFile(String fileId, String path, Cloud cloud) {
        setRefreshToken(cloud.getRefreshToken());
        try {
            deleteRequest(path, fileId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            deleteRequest(path, fileId);
        }
    }

    @Override
    public void renameFile(String fileName, String fileId, String newName, String path, Cloud cloud) {
        setRefreshToken(cloud.getRefreshToken());
        try {
            renameRequest(newName, fileId);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            renameRequest(fileName, fileId);
        }
    }

    @Override
    public File downloadLocal(String fileName, String path, String downloadUrl, Cloud cloud) {
        File file = new File(DOWNLOAD_PATH + System.currentTimeMillis() + fileName);
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
        System.out.println("upload next: Result - status (" + response.getStatusCode() + ") ");
        System.out.println("getBody: " + response.getBody());
    }


    @Override
    public boolean copyFile(String pathSourse, String pathDest, String idSource, String idDest, Cloud cloud) {
        setRefreshToken(cloud.getRefreshToken());
        try {
            copyRequest(pathSourse, pathDest, idSource, idDest);
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
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
}
