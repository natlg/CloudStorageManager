class FilesProvider {
    constructor() {
        this.pathList = [];
        this.fullPath = "";
        this.parentId = "";
        //use id as property name for getting file
        this.filesObj = {};
        this.clouds = [];
    }

    getFilesList(cloudName, path, handleData) {

        console.log("getFilesList from path: " + path + ", cloudName: " + cloudName);
        var xhttp = new XMLHttpRequest();
        var self = this;
        xhttp.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                console.log("XMLHttpRequest answer is ready");

                var response = JSON.parse(xhttp.responseText);
                var responseFiles = response.files;
                self.parentId = response.parentId;
                console.log("parentId: " + self.parentId);

                var arrayLength = responseFiles.length;
                self.filesObj = {};
                console.log("arrayLength: " + arrayLength);
                for (var i = 0; i < arrayLength; i++) {
                    self.filesObj[responseFiles[i].id] = new FileMetadata(self.getNameFromPath(responseFiles[i].pathLower),
                        responseFiles[i].type, responseFiles[i].modified, responseFiles[i].size, responseFiles[i].id,
                        responseFiles[i].pathLower, responseFiles[i].parentId, responseFiles[i].downloadUrl);
                }
                self.parsePath(path);
                handleData(self.filesObj);
            }
            else {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState + "...");
            }
        };
        var params = "path=" + path + "&cloudName=" + cloudName;
        xhttp.open("POST", "http://localhost:8080/listfiles", true);
        //for get
        //xhttp.open("GET", "http://localhost:8080/dropbox", true);
        xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        //xhttp.setRequestHeader("Content-type", "application/json");
        console.log("send params: " + params);
        console.log("cloudName: " + cloudName);
        xhttp.send(params);
    }


    parsePath(path) {
        if (path && path !== undefined && path.length !== 0) {
            console.log("parsePath path: " + path);
            this.fullPath = path;
            console.log("got fullPath: " + this.fullPath);

            if (path.charAt(0) === "/") {
                path = path.substr(1);
            }
            this.pathList = path.split("/");
            console.log("path array: " + this.pathList);
        }
        else {
            this.pathList = [];
            this.fullPath = "";
        }
    }

    getNameFromPath(path) {
        console.log("getNameFromPath path: " + path);
        var name = path.split("/").pop();
        console.log("name: " + name);
        return name;
    }
}

class FileMetadata {
    constructor(name, type, modified, size, id, pathLower, parentId, downloadUrl) {
        this.name = name;
        this.type = type;
        this.modified = modified;
        this.id = id;
        this.pathLower = pathLower;
        this.size = size;
        this.parentId = parentId
        //null for Dropbox
        if (downloadUrl != "null") {
            this.downloadUrl = downloadUrl;
        }
        console.log("created metadata, pathLower: " + pathLower + ", type: " + type
            + ", modified: " + modified, +", size: " + size + ", id: " + id + ", name: " + name + ", parentId: " + parentId + ", downloadUrl: " + downloadUrl);
    }
}

