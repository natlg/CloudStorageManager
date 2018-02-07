class FilesProvider {
    constructor() {
        this.pathList = [];
        this.pathIdList = [];
        this.fullPath = "";
        this.parentId = "";
        //use id as property name for getting file
        this.filesObj = {};
        this.clouds = [];
    }

    printPathIdList() {
        console.log("printPathIdList");
        if (this.pathIdList.length > 0) {
            for (var i = 0; i < this.pathIdList.length; i++) {
                console.log(i + " name: " + this.pathIdList[i].name + ", id: " + this.pathIdList[i].id);
            }
        }
        else {
            console.log("empty");
        }
    }

    getFilesList(cloudName, path, id, handleData) {
        currentFolderId = id;
        console.log("getFilesList from path: " + path + ", cloudName: " + cloudName + ", id: " + id);
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
                    var fileName;
                    console.log("name: " + responseFiles[i].name);
                    //get from Google Drive only
                    if (notEmpty(responseFiles[i].name) === 1) {
                        console.log("not undefined ");
                        fileName = responseFiles[i].name;
                    }
                    else {
                        console.log("undefined ");
                        fileName = self.getNameFromPath(responseFiles[i].pathLower);
                    }
                    self.filesObj[responseFiles[i].id] = new FileMetadata(fileName,
                        responseFiles[i].type, responseFiles[i].modified, self.formatBytes(responseFiles[i].size), responseFiles[i].id,
                        responseFiles[i].pathLower, responseFiles[i].parentId, responseFiles[i].downloadUrl);
                }
                self.parsePath(path);
                handleData(self.filesObj);
            }
            else {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState + "...");
            }
        };
        var folderIdParam;
        if (notEmpty(id) === 1) {
            folderIdParam = "&folderId=" + id;
        }
        else {
            folderIdParam = "";
        }
        var params = "path=" + path + "&cloudName=" + cloudName + folderIdParam;
        xhttp.open("POST", "/listfiles", true);
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
        if (notEmpty(path) === 1) {
            var name = path.split("/").pop();
            console.log("name: " + name);
            return name;
        }
    }


    formatBytes(bytes) {
        if (notEmpty(bytes) === 0) {
            return;
        }
        var units = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
        let l = 0, n = parseInt(bytes, 10) || 0;
        while (n >= 1024) {
            n = n / 1024;
            l++;
        }
        return (n.toFixed(n >= 10 || l < 1 ? 0 : 1) + ' ' + units[l]);
    }
}


var FileType = {
    IMAGE: 'img/icon_image.png',
    AUDIO: 'img/icon_audio.png',
    VIDEO: 'img/icon_video.png',
    TEXT: 'img/icon_document.png',
    PRESENTATION: 'img/icon_file.png',
    COMPRESSED: 'img/icon_file.png',
    OTHER: 'img/icon_file.png',
    FOLDER: 'img/icon_folder.png'
};


class FileMetadata {
    getFileType(fileName) {
        if (this.type === 'folder') {
            return FileType.FOLDER;
        }
        var ext = (fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length) || fileName).toLocaleLowerCase();
        if (ext === 'jpeg' || ext === 'jpg' || ext === 'png' || ext === 'ai' || ext === 'bmp' || ext === 'gif' || ext === 'ico' || ext === 'ps'
            || ext === 'psd' || ext === 'svg' || ext === 'tif' || ext === 'tiff') {
            return FileType.IMAGE;
        }
        if (ext === 'mp3' || ext === 'wma' || ext === 'wav' || ext === 'aif' || ext === 'cda' || ext === 'mid' || ext === 'midi' || ext === 'mpa'
            || ext === 'ogg' || ext === 'wpl') {
            return FileType.AUDIO;
        }
        if (ext === 'mp4' || ext === 'mov' || ext === 'wmv' || ext === 'avi' || ext === 'm4v' || ext === '3g2' || ext === '3gp' || ext === 'flv' || ext === 'h264'
            || ext === 'mkv' || ext === 'mpg' || ext === 'mpeg' || ext === 'rm' || ext === 'swf' || ext === 'vob' || ext === 'wmv') {
            return FileType.VIDEO;
        }
        if (ext === 'pdf' || ext === 'txt' || ext === 'docx' || ext === 'doc' || ext === 'odt' || ext === 'rtf' || ext === 'tex' || ext === 'wks'
            || ext === 'wps' || ext === 'wpd') {
            return FileType.TEXT;
        }
        return FileType.OTHER;
    }

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
        this.fileType = this.getFileType(name);
        console.log("created metadata, pathLower: " + pathLower + ", type: " + type
            + ", modified: " + modified, +", size: " + size + ", id: " + id + ", name: " + name + ", parentId: " + parentId + ", downloadUrl: " + this.downloadUrl + ", fileType: " + this.fileType);
    }
}

