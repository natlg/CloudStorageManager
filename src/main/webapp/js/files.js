class FilesProvider {
    constructor() {
        this.pathList = [];
        this.fullPath = "";
    }

    getFilesList(path, handleData) {

        console.log("getFilesList from path: " + path);
        var xhttp = new XMLHttpRequest();
        var self = this;
        xhttp.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                console.log("XMLHttpRequest answer is ready");

                console.log("responseText: " + xhttp.responseText);
                console.log("parsed: " + JSON.parse(xhttp.responseText));
                console.log("files: " + JSON.parse(xhttp.responseText).files);

                var response = JSON.parse(xhttp.responseText).files;
                var arrayLength = response.length;
                var fileArray = [];
                console.log("arrayLength: " + arrayLength);
                for (var i = 0; i < arrayLength; i++) {
                    fileArray.push(new FileMetadata(self.getNameFromPath(response[i].pathLower), response[i].type, response[i].modified, response[i].size, response[i].id, response.pathLower));
                    console.log("push: " + i);
                }

                self.parsePath(path);
                handleData(fileArray);
            }
            else {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState + "...");
            }
        };
        var params = "path=" + path;
        xhttp.open("POST", "http://localhost:8080/dropbox", true);
        //for get
        //xhttp.open("GET", "http://localhost:8080/dropbox", true);
        xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        //xhttp.setRequestHeader("Content-type", "application/json");
        console.log("send params:");
        console.log(params);
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
    constructor(name, type, modified, size, id, pathLower) {
        this.name = name;
        this.type = type;
        this.modified = modified;
        this.id = id;
        this.pathLower = pathLower;
        this.size = size;
        console.log("created metadata, pathLower: " + pathLower + ", type: " + type
            + ", modified: " + modified, +", size: " + size + ", id: " + id);
    }
}

