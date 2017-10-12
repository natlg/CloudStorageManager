package com.nat.cloudman.response;

import java.util.HashSet;
import java.util.Set;

public class CloudContainer {
    private Set<Cloud> clouds = new HashSet<>();
    private String userEmail;
    private String userFirstName;
    private String userLastName;

    public CloudContainer(Set<Cloud> clouds, String userEmail, String userFirstName, String userLastName) {
        this.clouds = clouds;
        this.userEmail = userEmail;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
    }

    public CloudContainer(String userEmail, String userFirstName, String userLastName) {
        this.userEmail = userEmail;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
    }

    public void addCloud(String accountName, String service) {
        Cloud cloud = new Cloud(accountName, service);
        clouds.add(cloud);
    }

    public Set<Cloud> getClouds() {
        return clouds;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    private class Cloud {
        public String accountName;
        public String service;

        public Cloud(String accountName, String service) {
            this.accountName = accountName;
            this.service = service;
        }
    }
}
