package com.nat.cloudman.controllers.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class FileParameters {
    public String fileName = "";
    public String fileId = "";
    public String cloudName = "";
    public String path = "";
    public String newName = "";

    @Override
    public String toString() {
        return "FileParam fileName: " + fileName + ", fileId: " + fileId + ", cloudName: " + cloudName + ", path: " + path + ", newName: " + newName;
    }
}
