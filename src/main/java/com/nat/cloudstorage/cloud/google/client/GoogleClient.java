package com.nat.cloudstorage.cloud.google.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.nat.cloudstorage.cloud.CloudCredentials;
import com.nat.cloudstorage.controllers.CloudController;
import com.nat.cloudstorage.response.FilesContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GoogleClient.class);

    public GoogleClient(GoogleConfig config) {
        this.config = config;
    }

    // TODO: use java library

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
        logger.debug("request.getBody: " + request.getBody() + "request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            String accessToken = getResponseProperty(response, "access_token");
            String expiresIn = getResponseProperty(response, "expires_in");
            logger.debug("got access_oken: " + accessToken + ", expires_in: " + expiresIn);
            return accessToken;
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getStatusText: " + e.getStatusText());
            e.printStackTrace();
        }
        return null;
    }

    public String getRootIdRequest(String accessToken) {
        logger.debug("getRootIdRequest");
        String url = "https://www.googleapis.com/drive/v3/files/" + "root";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        logger.debug("request.getBody: " + request.getBody());
        logger.debug("request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
        String rootId = getResponseProperty(response, "id");
        logger.debug("id : " + rootId);
        return rootId;
    }

    public FilesContainer getFilesListRequest(String folderId) {
        logger.debug("getFilesListRequest");
        if (folderId == null || folderId.isEmpty()) {
            folderId = getRootIdRequest(config.getAccessToken());
        }
        String url = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "' in parents";
        logger.debug("url: " + url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + config.getAccessToken());
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        logger.debug("request.getBody: " + request.getBody());
        logger.debug("request.getHeaders: " + request.getHeaders());
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        logger.debug("Result - status (" + response.getStatusCode() + ") ");
        logger.debug("getBody: " + response.getBody());
        logger.debug("files: " + getResponseProperty(response, "files"));
        JsonNode valueNode = response.getBody().path("files");
        Iterator<JsonNode> iterator = valueNode.iterator();
        logger.debug("files:");
        ArrayList<HashMap<String, String>> files = new ArrayList<HashMap<String, String>>();
        while (iterator.hasNext()) {
            JsonNode file = iterator.next();
            logger.debug("name: " + file.get("name").asText());
            HashMap<String, String> resultFile = new HashMap<String, String>();
            if (file.get("mimeType").asText().equals("application/vnd.google-apps.folder")) {
                resultFile.put("type", "folder");
                logger.debug("folder: " + file.get("name").asText());
            } else {
                resultFile.put("type", "file");
                logger.debug("file: " + file.get("name").asText());
            }
            resultFile.put("name", file.get("name").asText());
            resultFile.put("id", file.get("id").asText());
            files.add(resultFile);
        }
        logger.debug("listChildrenRequest return len: " + files.size());
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
            return new CloudCredentials(accessToken, refreshToken);
        } catch (HttpClientErrorException e) {
            logger.debug("HttpClientErrorException: " + e.getMessage() + " getStatusText: " + e.getStatusText());
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
