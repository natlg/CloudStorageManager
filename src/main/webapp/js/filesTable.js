function allowDrop(ev) {
    ev.preventDefault();
}

function drag(ev) {
    ev.dataTransfer.setData("text", ev.target.id);
}

function drop(ev) {
    ev.preventDefault();
    //  var data = ev.dataTransfer.getData("text");
    //TODO
    // ev.target.appendChild(document.getElementById(data));
}

function dblclickFile(event) {
    console.log("dblclickFile");
    console.log("data id: " + event.data.id + ", name: " + event.data.name);
    var folderPath = $(document.getElementById(event.data.id)).find(".fileName").text();
    console.log("textContent: " + folderPath);
    console.log("fullPath: " + filesProvider.fullPath);
    var pathToShow = filesProvider.fullPath + "/" + folderPath;
    console.log("pathToShow: " + pathToShow);
    var folderToShow = {name: event.data.name, id: pathIdPref + event.data.id};
    filesProvider.pathIdList.push(folderToShow);
    filesProvider.printPathIdList();
    listFolder(currentCloud, pathToShow, event.data.id);
}

function pathClick() {
    console.log("pathClick");
    var clickedPath = $(this).text();
    console.log("text: " + clickedPath);
    console.log("class: " + $(this).attr('class'));
    if ($($(this)).attr('class').toString().indexOf("mainFolder") !== -1) {
        console.log("mainFolder ");
        listFolder(currentCloud, "", "");
        filesProvider.pathIdList = [];
        return;
    }

    var path = "/";

    // prevAll works vice versa
    var prevLinkArray = Array.prototype.reverse.call($($(this).prevAll(".pathFolder")));
    console.log("path length: " + prevLinkArray.length);
    // first element is name of the Cloud
    if (prevLinkArray.length > 0) {
        for (var i = 0; i < prevLinkArray.length; i++) {
            console.log("prev: " + $(prevLinkArray[i]).text());
            path += $(prevLinkArray[i]).text() + "/";
        }
    }
    path += clickedPath;
    console.log("all path: " + path);
    var id = $(this).attr('id').substring(pathIdPref.length);
    console.log("path click id: " + id)
    listFolder(currentCloud, path, id);
}

// gets id of clicked file
function clickDetails(event) {
    console.log("clickDetails");
    console.log("data id: " + event.data.id);
}

//bind popover to dynamic elements
function bindPopover() {
    console.log("bindPopover  ");

    $('body').popover({
        selector: '[rel=popover]',
        trigger: 'focus',
        content: detailsMenuContent,
        placement: "left",
        html: true
    });
}

function getText(text) {
    if (text) {
        return text;
    }
    else
        return "";
}

function listFolder(cloudName, path, id) {
    if (filesProvider) {
        console.log("current parent: " + filesProvider.fullPath);
    }
    else {
        console.log("filesProvider doesn't exist");
        filesProvider = new FilesProvider();
    }
    filesProvider.getFilesList(cloudName, path, id, handleFile);
}

//gets clicked row information when clicking details
function rowClick(event) {
    fileIdPopover = event.data.id;
    fileNamePopover = $(document.getElementById(event.data.id)).find(".fileName").text();
    console.log("rowClick data id: " + fileIdPopover + ", fileName: " + fileNamePopover);
}

function getThumbnail(currentCloud, id, pathLower) {
    //TODO for dropbox
    console.log("getThumbnail path: " + pathLower);
    var params = {
        fileId: id,
        path: pathLower,
        cloudName: currentCloud
    };
    callMethod("/getthumbnail", "POST", params, "", function (response) {
        console.log("got thumbnail, response:" + response);
        if (notEmpty(response) === 1) {
            console.log("got thumbnail");
            var imgThumb = document.getElementById(id).getElementsByTagName('img')[0];
            imgThumb.src = response;
            $(imgThumb).css('width', 'auto');
            $(imgThumb).css('height', '50px');
        }
        else {
            console.log("didn't get thumbnail");
        }
    });
}

function addFilesToTable(files, type, table, isShowThumbnails) {
    for (var key in files) {
        if (files[key].type !== type) {
            //skip
            continue;
        }
        if (files.hasOwnProperty(key)) {
            var fileId = files[key].id;
            var fileName = files[key].name;
            console.log("add: " + fileName);
            var fileStyle = ``;
            if (files[key].type == "file") {
                fileStyle = `style=' pointer-events: none;'`;
            }
            var row =
                `<tr class="context_popup" data-toggle="popover" rel=context-popover id=${fileId} ondrop="drop(event)" ondragover="allowDrop(event)" draggable="true"
                    ondragstart="drag(event)">
                        <td class="middle-text" style=" padding-left: 20px"> <img class="icon" src="${files[key].fileType}"><a class="fileName table-text" href="#" ` + fileStyle + `>${fileName}</a></td>
                        <td class="table-text middle-text">${files[key].type}</td>
                        <td class="table-text middle-text">${getText(files[key].size)}</td>
                        <td class="table-text middle-text">${getText(files[key].modified)}</td>
                    <td class="middle-text" style=" padding-right: 20px">
                    <a tabindex="0" role="button" href="#!" class="hoverAble details_btn" data-toggle="popover" rel="popover" data-placement="left"
                    data-popover-content="#popoverContent"
                    data-trigger="focus">
                ...</a>
                    </td>
                    </tr>`;

            table.append($(row));
            console.log("id: " + fileId);
            var r = $(document.getElementById(fileId));
            r.on("click", {id: fileId}, rowClick);
            if (files[key].type == "folder") {
                r.on("dblclick", {id: fileId, name: fileName}, dblclickFile);
                r.find("a.fileName").on("click", {id: fileId, name: fileName}, dblclickFile);
            }
            var link = r.find('a');
            bindPopover();
            rowId = "";
            if (!(isShowThumbnails === 1) && (files[key].fileType === FileType.IMAGE)) {
                getThumbnail(currentCloud, files[key].id, files[key].pathLower);
            }
        }
    }

}

// callback function after getting answer from server
function handleFile(files) {
    console.log("handleFile ");
    var table = $("#filesTableBody");
    $("#files_table").show();

    //remove rows after previous click
    table.empty();
    //no thumbnails for Dropbox as no src for it
    var isDropbox = 0;
    filesProvider.clouds.forEach(function (cl) {
        if (cl.accountName === currentCloud && cl.service === 'Dropbox') {
            isDropbox = 1;
            return;
        }
    });
    // show folders first, so add them first. then add files
    addFilesToTable(files, "folder", table, isDropbox);
    addFilesToTable(files, "file", table, isDropbox);
    var pathContainer = $("#pathContainer");
    emptyPath();

    //TODO multy requests

    if (filesProvider.pathIdList.length > 0) {
        for (var i = 0; i < filesProvider.pathIdList.length; i++) {
            console.log(i + " name: " + filesProvider.pathIdList[i].name);
            //starts with 'path_' to not duplicate row id
            var pathId = filesProvider.pathIdList[i].id.substring(pathIdPref.length);
            if (pathId === filesProvider.parentId) {
                console.log(" parent folder: " + filesProvider.pathIdList[i].name + ", id: " + filesProvider.pathIdList[i].id);
                filesProvider.pathIdList.splice(i + 1, filesProvider.pathIdList.length + 1);
                break;
            }
        }
    }

    // create path links
    if (filesProvider.pathIdList && filesProvider.pathIdList !== undefined && filesProvider.pathIdList.length > 0) {
        for (var e = 0; e < filesProvider.pathIdList.length; e++) {
            console.log("pathlink e: " + e + ", id: " + filesProvider.pathIdList[e].id + ", name: " + filesProvider.pathIdList[e].name);
            var fileLink = $(`<i class="fa fa-angle-right"></i>
            <a href="#" id="${filesProvider.pathIdList[e].id}" class="pathLink pathFolder pathText">${filesProvider.pathIdList[e].name}</a>`);
            pathContainer.append(fileLink);
            fileLink.on("click", pathClick);
        }
    }
}

function emptyPath() {
    var pathContainer = $("#pathContainer");
    pathContainer.empty();
    var mainFileLink = $(`<a class="pathLink mainFolder pathText" href="#" >${currentCloud}</a>`);
    pathContainer.append(mainFileLink);
    mainFileLink.on("click", pathClick);
}

function clickUpload() {
    console.log("upload");
    var input = document.getElementById("uploadFilesInput");
    var files = input.files;
    uploadFiles(files);
}

function uploadFiles(files) {
    var formData = new FormData();
// Loop through each of the selected files.
    for (var i = 0; i < files.length; i++) {
        var file = files[i];
        // Add the file to the request.
        formData.append('files', file, file.name);
        console.log("append file: " + file.name);
    }
    formData.append("filePath", filesProvider.fullPath);
    formData.append("cloudName", currentCloud);
    formData.append("parentId", filesProvider.parentId);

    var xhr = new XMLHttpRequest();
    xhr.open('POST', domainName + '/upload/', true);
    // Set up a handler for when the request finishes.
    xhr.onload = function () {
        if (xhr.status === 200) {
            // File(s) uploaded.
            console.log("UPLOADED! ");
            listFolder(currentCloud, filesProvider.fullPath, filesProvider.parentId);
        } else {
            console.log("error on upload request");
        }
    };
    // Send the Data.
    console.log("Start uploading");
    showTempAlert("Start uploading", 'info');
    xhr.send(formData);
}
