package com.nat.cloudstorage.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class FolderParameters {
    public String path = "";
    public String cloudName = "";
    public String folderName = "";
    public String parentId = "";

    public FolderParameters() {
    }

    public FolderParameters(String path, String cloudName, String folderName, String parentId) {
        this.path = path;
        this.cloudName = cloudName;
        this.folderName = folderName;
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "FolderParameters, path: " + path + ", cloudName: " + cloudName + ", folderName: " + folderName +
                ", parentId: " + parentId;
    }
}
