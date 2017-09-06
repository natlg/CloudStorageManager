package com.nat.cloudman.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class OneDriveUtils {

    @Value("${onedrive.app.key}")
    private String APP_KEY;

    @Value("${onedrive.app.secret}")
    private String APP_SECRET;

    private String accessToken;
    private String refreshToken;

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
            String refreshToken = response.getBody().get("refresh_token").asText();
            String access_oken = response.getBody().get("access_token").asText();
            String expires_in = response.getBody().get("expires_in").asText();
            System.out.println("got refreshToken: " + refreshToken);
            System.out.println("got access_oken: " + access_oken + ", expires_in: " + expires_in);
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
            String refreshToken = response.getBody().get("refresh_token").asText();
            String access_oken = response.getBody().get("access_token").asText();
            String expires_in = response.getBody().get("expires_in").asText();
            System.out.println("got token: " + refreshToken);
            System.out.println("got access_oken: " + access_oken + " expires_in: " + expires_in);
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
            String gotRefreshToken = response.getBody().get("refresh_token").asText();
            String access_oken = response.getBody().get("access_token").asText();
            String expires_in = response.getBody().get("expires_in").asText();
            System.out.println("gotRefreshToken: " + gotRefreshToken);
            System.out.println("access_oken: " + access_oken + ", expires_in: " + expires_in);
            return access_oken;
        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());
        }
        return null;
    }

    public ArrayList<HashMap<String, String>> getFilesList(String folderPath) {

        System.out.println("oneDriveUtils. getFilesList");
        System.out.println("refreshToken: " + refreshToken);
        System.out.println("accessToken: " + accessToken);
        try {
            return listChildrenRequest(folderPath);

        } catch (HttpClientErrorException e) {
            System.out.println("HttpClientErrorException: " + e.getMessage() + " getResponseBodyAsString: "
                    + e.getResponseBodyAsString() + " getStatusText: " + e.getStatusText()
                    + " getStackTrace: " + e.getStackTrace());

            accessToken = getAccessToken(refreshToken);
            setAccessToken(accessToken);
            return listChildrenRequest(folderPath);
        }
    }


    public ArrayList<HashMap<String, String>> listChildrenRequest(String folderPath) {
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
        System.out.println("value get: " + response.getBody().get("value").asText());
        System.out.println("value path: " + response.getBody().path("value").asText());

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
            files.add(resultFile);
        }
        System.out.println("listChildrenRequest return len: " + files.size());
        return files;
    }

    public static void main(String[] args) {
        String accessToken = "EwBAA8l6BAAU7p9QDpi/D7xJLwsTgCg3TskyTaQAAalrhYbyUHh8z63NibrtFPGDLiKdBWTPE3QC9lVRUnvqNwuJyl0qjVJrhNpTeP62EP5sHPOSl7J15UMbywOny9jEq7vO1+xfc/Vezy5xAKUkyjYtloeDesvsLteUvZtxsp+DGGjWkvansXy6x3Eav3+ZoeWd7kbu7FwAEJmMZaF0ASeGQPfjG09JzBm7Ilj0TzMg2iBwaK6RfCQBGqPB4TQDnUFapkYCPKbfnbJQ12sdWvdXNxpCywPdwTg7POA1ISSW0nBQ3YRu9P13VpQRkS9qywRQwIAvEnW3Dd467taYyzaeMcxyWNjG1HkJOlHh/8yAX90ApAbtvieekd/8LpcDZgAACBDuJn9ZRFynEAINNqy1QX0nGJ4Jedq0eRh6pdyXDveU4HE+nJU0TYRY1v99S2VzA8Fg0B3Z6XWwjmMuIVDX5wCOuZ2JkbNHAoGA8JXzhCvzjrwabg5Y0zKMy5+8S8KUV/dTtR/A3J5WESpgTsXfsGmvtCx2VGSoz/sqagJTaprwFVvy+Sswz7KgZAEtTJg1q8HDsxJbpuRpfJZ3Yvm/l32Ch1NXKhs6q16NL4BDIi5Gqo/YPPrHRw+kGnLkug3QE6Pvkeiys1jRsNE4vTV3/J3iquDEOQ3bqLtMqtFMpvKwivQ9e/LOmltCt8vcy3Kayb3o5ZZzr2FtRV7ka2vt9PCBbFhWRAosNcZ1aGMkNwX98QnOMrz6KUJYJf5NOjEPU3LTHO8oKHrUtKv0NuPU6Zq8FiKaGNLl1cvInfDY3bbvTt+CriHFpQD/XlV/e/+pBB224HB8prqxkeexYGKY92rHnwPjMGBdidbUpQ5MIEvSlNXZFsmtJJTATcUBMe85xycm3EgrRSu40HOYG8rcAwM4+DW9d1OEmpOF2D9QQgAS0HPo8/hiDyRmKzNco8/JHCWduYsisd0Hdntc8/3lDx9g3SL7MxQyjbA1Xgp8eOFsyOcMRpQ9BM9F9BrEnzfbtkdoRnu05OVe/Y1qdesSp0hpmwcVZ4bIEFSttOrGheVanKKezlhSzuOzTdhkBJ4Wnz0fGw226dURY8hEAg==";
        String refreshToken = "MCVncBlg6Frfu2G3tB9!kmnveR1PEO2BcXpxx37uwT4!cX68nW3CJdUDD!Q91ZSVAv035DZmidb92rLP609WArg2BISBAFID!qICI1j!aWYFeB94tuZtDjz4BGWiOMsjiL6*8JFsNY93CsPlRqcEKcdls!H4HMSakKdZiEgPJRbmPZBpt0yKkr3fm6Kg5QDDDB0VQ9C*z3TcWs2mI6ebvveJ7a!FE6DuDgXuj1FZvQcNiZehaDAn4ICz9bv7aQn*7iz9xTX3V7PrcIu5wS63YlIeMIE*tz4EsAF3aw*4oPQHaipPzMYjCTp0UHf3Hd8g*oqec9W*XVmLpIlBlu6xuDQxTfSVJd9iKMziNEh4mgazJ";
        OneDriveUtils oneDrive = new OneDriveUtils();
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

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
