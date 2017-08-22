package com.nat.cloudman.response;

import java.util.ArrayList;
import java.util.HashMap;

public class FilesContainer {

    private  ArrayList<HashMap<String, String>> files;

    public FilesContainer(ArrayList<HashMap<String, String>> files) {
        this.files = files;
    }

    public ArrayList<HashMap<String, String>> getFiles() {
        return files;
    }
}
