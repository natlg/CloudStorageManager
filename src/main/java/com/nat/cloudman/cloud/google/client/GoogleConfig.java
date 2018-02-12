package com.nat.cloudman.cloud.google.client;

public class GoogleConfig {
    public String CLIENT_ID;
    public String CLIENT_SECRET;
    private String accessToken;
    private String refreshToken;

    public GoogleConfig(String CLIENT_ID, String CLIENT_SECRET, String accessToken, String refreshToken) {
        this.CLIENT_ID = CLIENT_ID;
        this.CLIENT_SECRET = CLIENT_SECRET;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}