package com.nat.cloudman.response;

import java.util.ArrayList;
import java.util.HashMap;

public class FilesContainer {

    private ArrayList<HashMap<String, String>> files;
    private String parentId;

    public FilesContainer(ArrayList<HashMap<String, String>> files, String parentId) {
        this.files = files;
        this.parentId = parentId;
    }

    public ArrayList<HashMap<String, String>> getFiles() {
        return files;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
