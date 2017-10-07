var filesProvider;
var currentCloud;
var fileIdPopover;
var fileNamePopover;
var rowId;
var selectedNode;
var detailsMenuContent = `<div id="popoverContent" class="borderless popoverDetails">
        <a id="pop_copy" href="#" class="list-group-item" data-toggle="modal" data-target="#modalCopy">Copy</a>
        <a id="pop_move" href="#" class="list-group-item">Move</a>
        <a id="pop_rename" href="#" class="list-group-item" data-toggle="modal" data-target="#modalRename">Rename</a>
        <a id="pop_delete" href="#" class="list-group-item">Delete</a>
        <a id="pop_download" href="#" class="list-group-item">Download</a>
        </div>`;


function isAuthorized() {
    var isAuthorized = getFromSessionStorage("isAuthorized");
    //if not set yet
    if (typeof isAuthorized == "undefined") {
        setAuthorized(false);
        return false;
    }
    else {
        console.log("isAuthorized: " + isAuthorized);
        return isAuthorized;
    }
}

function setAuthorized(auth) {
    saveToSessionStorage("isAuthorized", auth)
}

function saveToSessionStorage(key, value) {
    if (typeof(Storage) !== "undefined") {
        sessionStorage[key] = value;
    } else {
        console.log("No Web Storage support");
    }
}

function getFromSessionStorage(key) {
    if (typeof(Storage) !== "undefined") {
        return sessionStorage[key];
    } else {
        console.log("No Web Storage support");
    }
}

function removeFromSessionStorage(key) {
    if (typeof(Storage) !== "undefined") {
        sessionStorage.removeItem(key);
    } else {
        console.log("No Web Storage support");
    }
}

function setHeader() {
    var url = document.URL;
    if (isAuthorized() == 'true') {
        $('.header').load("templates/homeheader.html");
    }
    else if (url.indexOf("login") >= 0) {
        $('.header').load("templates/loginheader.html");
    }
    else {
        $('.header').load("templates/mainheader.html");
    }
}

function getDropboxRedirectParams(url) {
    // get query string from url (
    var queryString = url ? url.split('?')[1] : window.location.search.slice(1);
    // store the parameters
    var obj = {};
    if (queryString) {

        // params are after #
        if (url.indexOf("redirect=dropbox") >= 0) {
            queryString = queryString.split('#')[1];
        }

        // split our query string into its component parts
        var arr = queryString.split('&');
        for (var i = 0; i < arr.length; i++) {
            // separate the keys and the values
            var a = arr[i].split('=');

            // in case params look like: list[]=thing1&list[]=thing2
            var paramNum = undefined;
            var paramName = a[0].replace(/\[\d*\]/, function (v) {
                paramNum = v.slice(1, -1);
                return '';
            });

            // set parameter value (use 'true' if empty)
            var paramValue = typeof(a[1]) === 'undefined' ? true : a[1];

            // if parameter name already exists
            if (obj[paramName]) {
                // convert value to array (if still string)
                if (typeof obj[paramName] === 'string') {
                    obj[paramName] = [obj[paramName]];
                }
                // if no array index number specified...
                if (typeof paramNum === 'undefined') {
                    // put the value on the end of the array
                    obj[paramName].push(paramValue);
                }
                // if array index number specified...
                else {
                    // put the value at that index number
                    obj[paramName][paramNum] = paramValue;
                }
            }
            // if param name doesn't exist yet, set it
            else {
                obj[paramName] = paramValue;
            }
        }
    }
    return obj;
}

function loadAuthorizedPage() {
    setHeader();
    var url = document.URL;
    var cloud = getFromSessionStorage("added_cloud_drive");

    // send request to add cloud if it's auth redirection
    if (isAuthorized() == 'true' && cloud !== undefined && cloud != null && cloud.length >= 0) {
        if (cloud.indexOf("OneDrive") >= 0) {
            var params = getDropboxRedirectParams(url);
            var code = params.code;
            console.log("code: " + code);
            var cloud = getFromSessionStorage("added_cloud_drive");
            var cloudName = getFromSessionStorage("added_cloud_name");
            sendAddCloudRequest(cloud, cloudName, code);
        }
        else if (cloud.indexOf("Dropbox") >= 0) {
            var params = getDropboxRedirectParams(url);
            console.log("access_token: " + params.access_token);
            console.log("token_type: " + params.token_type);
            console.log("uid: " + params.uid);
            console.log("account_id: " + params.account_id);
            var cloud = getFromSessionStorage("added_cloud_drive");
            var cloudName = getFromSessionStorage("added_cloud_name");
            var token = params.access_token;
            sendAddCloudRequest(cloud, cloudName, token);
        }
        removeFromSessionStorage("added_cloud_drive");
    }
}

function copy() {

    var nameSource = filesProvider.filesObj[fileIdPopover].name;
    var cloudSource = currentCloud;
    var pathSource = filesProvider.fullPath + "/" + nameSource;
    var idSource = filesProvider.filesObj[fileIdPopover].id;
    console.log("copy nameSource: " + nameSource + ", cloudSource: " + cloudSource + ", idSource: " + idSource + ", pathSource: " + pathSource);

    var pathDest = getPathFromNode(selectedNode) + "/" + nameSource;
    var cloudDest = getCloudFromNode(selectedNode);
    var idDest = selectedNode.key;
    console.log("copy pathDest: " + pathDest + ", cloudDest: " + cloudDest + ", idDest: " + idDest);


    var params = {
        cloudSource: cloudSource,
        pathSource: pathSource,
        idSource: idSource,
        cloudDest: cloudDest,
        pathDest: pathDest,
        idDest: idDest
    };

    callMethod("http://localhost:8080/copy", params, function (response) {
        console.log("Copied");
    });

}

function callMethod(url, parameters, successCallback) {
    //show loading... image

    $.ajax({
        type: 'POST',
        url: url,
        data: JSON.stringify(parameters),
        contentType: 'application/json;',
        dataType: 'json',
        success: function (data) {
            console.log("success");
            successCallback(data);
        },
        error: function () {
            console.log('error');
        }
    });
}

function getPathFromNode(node) {
    var parents = node.getParentList(false, true);
    var path = "";
    if (parents.length > 1) {
        //0th is cloud name
        for (var i = 1; i < parents.length; i++) {
            path += "/" + parents[i].title;
        }
    }
    console.log(" path: " + path);
    return path;
}

function getCloudFromNode(node) {
    var parents = node.getParentList(false, true);
    if (parents.length > 0) {
        var cloud = parents[0].title;
        console.log(" cloud: " + cloud);
        return cloud;
    }
}

function copyClick() {
    fillTree();
}

function fillTree() {
    var source = [];
    console.log(" fillTree: ");
    for (var i = 0; i < filesProvider.clouds.length; i++) {
        var cloud = {};
        cloud.title = filesProvider.clouds[i].accountName;
        cloud.folder = true;
        cloud.lazy = true;
        source.push(cloud);
    }
    console.log(" source: " + source);

    $(function () {
        $("#tree").fancytree({
            checkbox: false,
            selectMode: 3,
            source: source,
            lazyLoad: function (event, data) {
                var cloud = getCloudFromNode(data.node);
                var path = getPathFromNode(data.node);
                console.log(" path: " + path + " cloud: " + cloud);
                var json = JSON.stringify({cloudName: cloud, path: path, id: ""});

                data.result = {
                    url: "http://localhost:8080/getcloudstree",
                    cache: false,
                    type: 'POST',
                    data: json,
                    contentType: 'application/json; charset=utf-8',
                    success: function (response) {
                        console.log("lazyLoad responseObj: " + response.toString());
                    },
                    error: function () {
                        console.log("lazyLoad error");
                    }
                };
            },

            //convert response to the tree format
            postProcess: function (event, data) {
                data.result = convertData(data.response);
            },
            activate: function (event, data) {

            },
            select: function (event, data) {

            },
            focus: function (event, data) {
                console.log("focus type: " + event.type + +data.node.isSelected() +
                    " title:" + data.node.title);
                //save selected node for case it will be copied
                selectedNode = data.node;
            }
        });
    });
}

function convertData(filesObj) {
    var file;
    var sourceFiles = [];
    for (file of filesObj.files) {
        console.log("show file: " + file.title)
        // need only folders for copy|move
        if (file.type == "folder") {
            file.title = filesProvider.getNameFromPath(file.displayPath);
            file.key = file.id;
            file.lazy = true;
            file.folder = true;
            sourceFiles.push(file);
        }
    }
    return sourceFiles;
}

function move() {

}

function renameClick() {
    var name = filesProvider.filesObj[fileIdPopover].name;
    $("#new_name").val(name);
    console.log("renameClick fileIdPopover: " + fileIdPopover + ", name: " + name);
}

function rename() {
    var name = filesProvider.filesObj[fileIdPopover].name;
    var newName = $("#new_name").val();
    console.log("rename fileIdPopover: " + fileIdPopover + ", name: " + name + ", newName: " + newName);

    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
        }
        else {
            if (this.readyState === 4 && this.status !== 200) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
            }
        }
    };
    var params = "fileId=" + fileIdPopover +
        "&fileName=" + name +
        "&newName=" + newName +
        "&path=" + filesProvider.fullPath +
        "&cloudName=" + currentCloud;
    xhttp.open("POST", "http://localhost:8080/renamefile", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
}

function deleteFile() {
    var name = filesProvider.filesObj[fileIdPopover].name;
    console.log("deleteFile fileIdPopover: " + fileIdPopover + ", name: " + name);

    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
        }
        else {
            if (this.readyState === 4 && this.status !== 200) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
            }
        }
    };

    var params = "fileId=" + fileIdPopover +
        "&fileName=" + name +
        "&cloudName=" + currentCloud;
    xhttp.open("POST", "http://localhost:8080/deletefile", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
}

function download() {
    var downloadPath = filesProvider.filesObj[fileIdPopover].downloadUrl;
    console.log("download path: " + downloadPath);
    //exists for all OneDrive files
    if (notEmpty(downloadPath) === 1) {
        window.location = downloadPath;
        return;
    }

    var params = "fileName=" + fileNamePopover +
        "&cloudName=" + currentCloud + "&fileId=" + fileIdPopover +
        "&path=" + filesProvider.fullPath;

    console.log("send params: " + params);
    // download from browser after getting server response
    window.location = "/downloadFile?" + params;
}

$(document).ready(function () {
    console.log("document ready");
    filesProvider = new FilesProvider();
    getClouds();


    $(document).on('click', '#login', function () {
        location.href = "login.html";
    });

    $(document).on('click', '#logout', logoutClick);

    $(document).on('click', '#add_cloud', addCloud);
    $(document).on('click', '#add_folder', addFolder);

    $(document).on('click', '#pop_copy', copyClick);
    $(document).on('click', '#pop_move', move);
    $(document).on('click', '#pop_rename', renameClick);
    $(document).on('click', '#pop_delete', deleteFile);
    $(document).on('click', '#pop_download', download);

    $(document).on('click', '#rename_btn', rename);
    $(document).on('click', '#copy_btn', copy);
    // $(document).on('click', '.cloud_expand', cloudExpand);


});

function notEmpty(str) {
    if (str === undefined || str == null || str.length <= 0) {
        console.log("empty");
        return 0;
    }
    console.log("not empty");
    return 1;
}


