package com.nat.cloudstorage.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class FileParameters {
    public String fileName = "";
    public String fileId = "";
    public String cloudName = "";
    public String path = "";
    public String newName = "";
    public String parentId = "";

    public FileParameters() {
    }

    public FileParameters(String fileName, String fileId, String cloudName, String path, String newName, String parentId) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.cloudName = cloudName;
        this.path = path;
        this.newName = newName;
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "FileParam fileName: " + fileName + ", fileId: " + fileId + ", cloudName: " + cloudName +
                ", path: " + path + ", newName: " + newName + ", parentId: " + parentId;
    }
}
