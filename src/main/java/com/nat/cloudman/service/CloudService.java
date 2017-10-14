package com.nat.cloudman.service;

import com.nat.cloudman.model.Cloud;

public interface CloudService {
    void saveCloud(Cloud cloud);

    void addCloudToCurrentUser(String cloudDrive, String cloudName, String token);

    void removeCloud(String cloudName);
}
