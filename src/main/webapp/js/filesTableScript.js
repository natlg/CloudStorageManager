function allowDrop(ev) {
    ev.preventDefault();
}

function drag(ev) {
    ev.dataTransfer.setData("text", ev.target.id);
}

function drop(ev) {
    ev.preventDefault();
    var data = ev.dataTransfer.getData("text");
    ev.target.appendChild(document.getElementById(data));
}

function dblclickFile(event) {
    console.log("dblclickFile");
    console.log("data id: " + event.data.id);
    var folderPath = $("#" + event.data.id).find("#fileName").text();
    console.log("textContent: " + folderPath);
    // listFolder("/new folder");
    console.log("current parent: " + filesProvider.fullPath);
    var pathToShow = filesProvider.fullPath + "/" + folderPath;
    console.log("pathToShow: " + pathToShow);
    listFolder(currentCloud, pathToShow);
}

function pathClick() {
    console.log("pathClick");
    var clickedPath = $(this).text();
    console.log("text: " + clickedPath);
    console.log("class: " + $(this).attr('class'));
    if ($($(this)).attr('class').toString().indexOf("mainFolder") !== -1) {
        console.log("mainFolder ");
        listFolder(currentCloud, "");
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
    listFolder(currentCloud, path);
}

// gets id of clicked file
function clickDetails(event) {
    console.log("clickDetails");
    console.log("data id: " + event.data.id);
}

//bind popover to dynamic elements
function bindPopover() {
    var content = `<div id="popoverContent" class="borderless">
        <a href="#" class="list-group-item">Copy</a>
        <a href="#" class="list-group-item">Move</a>
        <a href="#" class="list-group-item">Rename</a>
        <a href="#" class="list-group-item">Delete</a>
        <a href="#" class="list-group-item">Download</a>
        </div>`;

    $('body').popover({
        selector: '[rel=popover]',
        trigger: 'focus',
        content: content,
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

function listFolder(cloudName, path) {
    if (filesProvider) {
        console.log("current parent: " + filesProvider.fullPath);
    }
    else {
        console.log("filesProvider doesn't exist");
        filesProvider = new FilesProvider();
    }
    filesProvider.getFilesList(cloudName, path, handleFile);
}

// callback function after getting answer from server
function handleFile(files) {
    var table = $("#filesTableBody");

    //remove rows after previous click
    table.empty();
    console.log("handleFile, files Length: " + files.length);
    for (var i = 0; i < files.length; i++) {
        var row =
            `<tr id=${files[i].id} ondrop="drop(event)" ondragover="allowDrop(event)" draggable="true"
                    ondragstart="drag(event)">
                        <td> <a id="fileName" href="#">${files[i].name}</a></td>
                        <td>${files[i].type}</td>
                        <td>${getText(files[i].size)}</td>
                        <td>${getText(files[i].modified)}</td>
                    <td>
                    <a  href="#" class="hoverAble" data-toggle="popover" rel="popover" data-placement="left"
                    data-popover-content="#popoverContent"
                    data-trigger="focus">
                ...</a>
                    </td>
                    </tr>`;

        table.append($(row));

        var r = $('#' + files[i].id);
        r.on("dblclick", {id: files[i].id}, dblclickFile);
        r.find("a").on("click", {id: files[i].id}, dblclickFile);

        var link = r.find('a');

        bindPopover();
        link.on("click", {id: files[i].id}, clickDetails);
    }

    var pathContainer = $("#pathContainer");
    emptyPath();

    if (filesProvider.pathList && filesProvider.pathList !== undefined && filesProvider.pathList.length > 0) {
        for (var e = 0; e < filesProvider.pathList.length; e++) {
            var fileLink = $(`<i class="fa fa-angle-right"></i>
            <a href="#" class="pathLink pathFolder">${filesProvider.pathList[e]}</a>`);
            pathContainer.append(fileLink);
            fileLink.on("click", pathClick);
        }
    }
}

function emptyPath() {
    var pathContainer = $("#pathContainer");
    pathContainer.empty();
    var mainFileLink = $(`<a class="pathLink mainFolder" href="#" >${currentCloud}</a>`);
    pathContainer.append(mainFileLink);
    mainFileLink.on("click", pathClick);
}

function clickUpload() {
    console.log("upload");
    var input = document.getElementById("uploadFilesInput");

    var files = input.files;
    var formData = new FormData();

// Loop through each of the selected files.
    for (var i = 0; i < files.length; i++) {
        var file = files[i];

        // Add the file to the request.
        formData.append('files', file, file.name);
        console.log("append file: " + file.name);
    }
    formData.append("dropboxPath", filesProvider.fullPath);
    formData.append("cloudName", currentCloud);

    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8080/upload/', true);
    //xhr.open('POST', 'http://localhost:8080/app/upload/', true);

    // Set up a handler for when the request finishes.
    xhr.onload = function () {
        if (xhr.status === 200) {
            // File(s) uploaded.
            console.log("UPLOADED! ");
        } else {
            alert('An error occurred!');
        }
    };
    // Send the Data.
    xhr.send(formData);
}
