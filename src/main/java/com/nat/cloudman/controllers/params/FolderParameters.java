package com.nat.cloudman.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class FolderParameters {
    public String path = "";
    public String cloudName = "";
    public String folderName = "";
    public String parentId = "";


    @Override
    public String toString() {
        return "FolderParameters, path: " + path + ", cloudName: " + cloudName + ", folderName: " + folderName +
                ", parentId: " + parentId;
    }
}
