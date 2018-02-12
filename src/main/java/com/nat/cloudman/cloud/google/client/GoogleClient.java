package com.nat.cloudman.cloud.google.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.nat.cloudman.cloud.CloudCredentials;
import com.nat.cloudman.response.FilesContainer;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GoogleClient {

    private GoogleConfig config;

    public GoogleClient(GoogleConfig config) {
        this.config = config;
    }

    public String requestNewAccessToken(String refreshToken) {
        String url = "https://www.googleapis.com/oauth2/v4/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", config.CLIENT_ID);
        map.add("client_secret", config.CLIENT_SECRET);
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

    public String getRootIdRequest(String accessToken) {
        System.out.println("getRootIdRequest");
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

    public FilesContainer getFilesListRequest(String folderId) {
        System.out.println("getFilesListRequest");
        if (folderId == null || folderId.isEmpty()) {
            folderId = getRootIdRequest(config.getAccessToken());
        }
        String url = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "' in parents";
        System.out.println("url: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + config.getAccessToken());
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

    public CloudCredentials sendAuthorizationCodeRequest(String code, String domain) {
        String url = "https://www.googleapis.com/oauth2/v4/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("client_id", config.CLIENT_ID);
        map.add("code", code);
        map.add("redirect_uri", domain + "/index.html");
        map.add("grant_type", "authorization_code");
        map.add("client_secret", config.CLIENT_SECRET);
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
