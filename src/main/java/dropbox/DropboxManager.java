package dropbox;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class DropboxManager {

    private  ArrayList<HashMap<String, String>> files;

    public DropboxManager(ArrayList<HashMap<String, String>> files) {
        this.files = files;
    }

    public ArrayList<HashMap<String, String>> getFiles() {
        return files;
    }
}
