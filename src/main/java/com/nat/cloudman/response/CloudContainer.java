package com.nat.cloudman.response;

import com.nat.cloudman.model.Cloud;

import java.util.Set;

public class CloudContainer {
    private Set<Cloud> clouds;

    public CloudContainer(Set<Cloud> clouds) {
        this.clouds = clouds;
    }

    public Set<Cloud> getClouds() {
        return clouds;
    }
}
