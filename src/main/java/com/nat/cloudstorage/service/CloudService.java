package com.nat.cloudstorage.service;

import com.nat.cloudstorage.model.Cloud;

public interface CloudService {
    void saveCloud(Cloud cloud);

    void addCloudToCurrentUser(String cloudDrive, String cloudName, String code);

    void removeCloud(String cloudName);
}
