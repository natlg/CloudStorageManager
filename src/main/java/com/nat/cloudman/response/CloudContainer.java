package com.nat.cloudman.response;

import com.nat.cloudman.model.Cloud;

import java.util.Set;

public class CloudContainer {
    private Set<Cloud> clouds;
    private String userEmail;
    private String userFirstName;
    private String userLastName;

    public CloudContainer(Set<Cloud> clouds, String userEmail, String userFirstName, String userLastName) {
        this.clouds = clouds;
        this.userEmail = userEmail;
        this.userFirstName = userFirstName;
        this.userLastName = userLastName;
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
}
