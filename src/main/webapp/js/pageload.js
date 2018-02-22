var filesProvider;
var currentCloud;
var fileIdPopover;
var fileNamePopover;
var rowId;
var selectedNode;
var domainName = "http://localhost:8080";
var currentFolderId = "";
var pathIdPref = "path_";

var detailsMenuContent = `<div id="popoverContent" class="borderless popoverDetails">
        <a id="pop_copy" href="#" class="list-group-item" data-toggle="modal" data-target="#modalCopy">Copy</a>
        <a id="pop_move" href="#" class="list-group-item">Move</a>
        <a id="pop_rename" href="#" class="list-group-item" data-toggle="modal" data-target="#modalRename">Rename</a>
        <a id="pop_delete" href="#" class="list-group-item">Remove</a>
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
        $('#nav_footer').load("templates/homeheader.html");
    }
    else if (url.indexOf("login") >= 0) {
        $('.header-log').load("templates/loginheader.html");
    }
    else {
        $('#nav_footer').load("templates/mainheader.html");
        $("#mainPageText").show();

    }
}

function getRedirectParams(url) {
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
    var cloudName = getFromSessionStorage("added_cloud_name");
    // send request to add cloud if it's auth redirection
    if (isAuthorized() == 'true' && cloud !== undefined && cloud != null && cloud.length >= 0) {
        if (cloud.indexOf("OneDrive") >= 0) {
            var params = getRedirectParams(url);
            var code = params.code;
            console.log("code: " + code);
            sendAddCloudRequest(cloud, cloudName, code);
        }
        else if (cloud.indexOf("Dropbox") >= 0) {
            var params = getRedirectParams(url);
            console.log("access_token: " + params.access_token);
            console.log("token_type: " + params.token_type);
            console.log("uid: " + params.uid);
            console.log("account_id: " + params.account_id);
            var token = params.access_token;
            sendAddCloudRequest(cloud, cloudName, token);
        }
        else if (cloud.indexOf("Google") >= 0) {
            console.log("google authorized: " + url);
            console.log("google cloud: " + cloud);
            var params = getRedirectParams(url);
            var code = params.code;
            console.log("code: " + code);
            //var cloud = getFromSessionStorage("added_cloud_drive");
            sendAddCloudRequest(cloud, cloudName, code);
        }
        removeFromSessionStorage("added_cloud_drive");
    }
}

function showTempAlert(text) {
    $(".alert-info").text(text);
    $('.alert-info').show();
    window.setTimeout(function () {
        $(".alert-info").fadeTo(500, 0).slideUp(500, function () {
            $(this).remove();
        });
    }, 3000);
}

function transfer(action) {
    var nameSource = filesProvider.filesObj[fileIdPopover].name;
    var cloudSource = currentCloud;
    var pathSource = filesProvider.fullPath + "/" + nameSource;
    var idSource = filesProvider.filesObj[fileIdPopover].id;
    console.log(action + " nameSource: " + nameSource + ", cloudSource: " + cloudSource + ", idSource: " + idSource + ", pathSource: " + pathSource);

    var pathDest = getPathFromNode(selectedNode) + "/" + nameSource;
    var cloudDest = getCloudFromNode(selectedNode);
    var idDest = getIdFromNode(selectedNode);
    var downloadUrl = filesProvider.filesObj[fileIdPopover].downloadUrl;
    console.log(action + " pathDest: " + pathDest + ", cloudDest: " + cloudDest + ", idDest: " + idDest + ", downloadUrl: " + downloadUrl);


    var params = {
        parentId: filesProvider.parentId,
        cloudSource: cloudSource,
        pathSource: pathSource,
        idSource: idSource,
        downloadUrl: downloadUrl,
        cloudDest: cloudDest,
        pathDest: pathDest,
        idDest: idDest
    };

    showTempAlert("Start " + action);
    callMethod("/" + action, "POST", params, function (response) {
        console.log("Finished " + action);
        if (action === 'move') {
            listFolder(currentCloud, filesProvider.fullPath, filesProvider.parentId);
        }
    });
}


function copy() {
    transfer('copy');
}

function removeCloud() {
    var cloud = currentCloud;
    console.log("remove cloud: " + cloud);
    var params = {
        cloudName: cloud
    };
    showTempAlert("Removing " + currentCloud);
    callMethod("/removecloud", "DELETE", params, function (response) {
        console.log("removed");
        $("#files_table").hide();
        getClouds();
    });
}

function callMethod(url, method, parameters, successCallback) {
    $.ajax({
        type: method,
        url: url,
        data: JSON.stringify(parameters),
        contentType: 'application/json;',
    })
        .done(function (data, status) {
            console.log("success");
            successCallback(data);
        })
        .fail(function (data, status, xhr) {
            var status = xhr.status;
            console.log('ajax fail, status: ' + status);
            if (status == 401 || status == 400) {
                console.log("go to login");
                location.href = "login.html";
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

function getIdFromNode(node) {
    var parents = node.getParentList(false, true);
    // 1th node is cloud, so id will be wrong
    if (parents.length > 1) {
        return parents[parents.length - 1].key;
    }
    else {
        return "";
    }
}

function copyClick() {
    console.log(" copyClick ");
    if (!$('#modalCopy').hasClass('in')) {
        $("#modalCopy").modal("show");
    }
    fillTree('copy');
}

function fillTree(tree) {
    var source = [];
    console.log(" fillTree: ");
    for (var i = 0; i < filesProvider.clouds.length; i++) {
        var cloud = {};
        cloud.title = filesProvider.clouds[i].accountName;
        cloud.folder = true;
        cloud.lazy = true;
        source.push(cloud);
        console.log(" accountName: " + filesProvider.clouds[i].accountName);
    }
    console.log(" source: " + source);

    $(function () {
        $("#" + tree + "Tree").fancytree({
            checkbox: false,
            selectMode: 3,
            source: source,
            lazyLoad: function (event, data) {
                var cloud = getCloudFromNode(data.node);
                var path = getPathFromNode(data.node);
                var fileId = getIdFromNode(data.node);
                if (notEmpty(fileId) === 0) {
                    fileId = "";
                }
                console.log(" path: " + path + " cloud: " + cloud + ", fileId: " + fileId);
                var json = JSON.stringify({cloudName: cloud, path: path, id: fileId});

                data.result = {
                    url: "/getcloudstree",
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
        console.log("show file: " + file.name + ",path: " + file.displayPath)
        // need only folders for copy|move
        if (file.type == "folder") {
            if (notEmpty(file.name) === 1) {
                file.title = file.name;
            }
            else {
                file.title = filesProvider.getNameFromPath(file.displayPath);
            }
            file.key = file.id;
            file.lazy = true;
            file.folder = true;
            sourceFiles.push(file);
        }
    }
    return sourceFiles;
}

function moveClick() {
    console.log(" copyClick ");
    if (!$('#modalMove').hasClass('in')) {
        $("#modalMove").modal("show");
    }
    fillTree('move');
}

function move() {
    transfer('move');
}

function deleteClick() {
    console.log("deleteClick");
    var name = filesProvider.filesObj[fileIdPopover].name;
    console.log("deleteClick fileIdPopover: " + fileIdPopover + ", name: " + name);
    $("#remove-file-text").text(name);
    $("#modalRemoveFile").modal("show");
}

function renameClick() {
    console.log("renameClick");
    var name = filesProvider.filesObj[fileIdPopover].name;
    //set default name
    $("#new_name").val(name);
    console.log("renameClick fileIdPopover: " + fileIdPopover + ", name: " + name);
    $("#modalRename").modal("show");
}

function rename() {
    var name = filesProvider.filesObj[fileIdPopover].name;
    var newName = $("#new_name").val();
    if (name === newName) {
        return;
    }
    console.log("rename fileIdPopover: " + fileIdPopover + ", name: " + name + ", newName: " + newName);
    var params = {
        fileId: fileIdPopover,
        fileName: name,
        newName: newName,
        path: filesProvider.fullPath,
        cloudName: currentCloud
    };

    showTempAlert("Start renaming");

    callMethod("/renamefile", "POST", params, function (response) {
        console.log("file is renamed");
        listFolder(currentCloud, filesProvider.fullPath, filesProvider.parentId);
    });
}

function deleteFile() {
    var name = filesProvider.filesObj[fileIdPopover].name;
    console.log("deleteFile fileIdPopover: " + fileIdPopover + ", name: " + name);
    var params = {
        fileId: fileIdPopover,
        fileName: name,
        path: filesProvider.fullPath + "/" + name,
        parentId: filesProvider.parentId,
        cloudName: currentCloud
    };
    showTempAlert("Start deleting");
    callMethod("/deletefile", "DELETE", params, function (response) {
        console.log("file is deleted");
        listFolder(currentCloud, filesProvider.fullPath, filesProvider.parentId);
    });


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
    showTempAlert("Start downloading..");
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
    $(document).on('click', '#aboutButton', function () {
        console.log("aboutButton click");
        $("#files_table").hide();
        $("#mainPageText").hide();
        $("#aboutPageText").show();
    });

    $(document).on('click', '#logout', logoutClick);

    $(document).on('click', '#add_cloud', addCloud);
    $(document).on('click', '#add_folder', addFolder);

    $(document).on('click', '#pop_copy', copyClick);
    $(document).on('click', '#pop_move', moveClick);
    $(document).on('click', '#pop_rename', renameClick);
    $(document).on('click', '#pop_delete', deleteClick);
    $(document).on('click', '#pop_download', download);

    $(document).on('click', '#rename_btn', rename);
    $(document).on('click', '#copy_btn', copy);
    $(document).on('click', '#move_btn', move);
    $(document).on('click', '#remove_cloud', removeCloud);
    $(document).on('click', '#remove_file', deleteFile);

//hide clouds container when some is chosen (on mobile version)
    $(document).on('click', '#cloud_container a', function () {
        console.log("collapse clouds 3");
        if ($('.navbar-toggle').css('display') != 'none') {
            $(".navbar-toggle").trigger("click");
        }
    });

    $('#remove-cloud').click(function (event) {
        console.log("remove-cloud click");
        //add cloud name to the dialog
        $('#remove-cloud-text').text(currentCloud);
    });

    $.contextMenu({
        selector: '.context_popup',
        items: {
            copy: {
                name: "Copy",
                callback: copyClick
            },
            move: {
                name: "Move",
                callback: moveClick
            },
            rename: {
                name: "Rename",
                callback: renameClick
            },
            delete: {
                name: "Remove",
                callback: deleteClick
            },
            download: {
                name: "Download",
                callback: download
            }
        },
        events: {
            show: function (options) {
                console.log("contextMenu show ");
                fileIdPopover = options.$trigger.attr("id");
                fileNamePopover = $(document.getElementById(fileIdPopover)).find(".fileName").text();
                console.log("fileIdPopover: " + fileIdPopover + ", fileNamePopover: " + fileNamePopover);
            }
        }
    });
});

function notEmpty(str) {
    if (str === undefined || str === null || str.length <= 0) {
        console.log("empty");
        return 0;
    }
    console.log("not empty");
    return 1;
}


