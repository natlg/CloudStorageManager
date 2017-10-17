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
    console.log("data id: " + event.data.id);
    var folderPath = $(document.getElementById(event.data.id)).find(".fileName").text();
    console.log("textContent: " + folderPath);
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

//gets clicked row information when clicking details
function rowClick(event) {
    fileIdPopover = event.data.id;
    fileNamePopover = $(document.getElementById(event.data.id)).find(".fileName").text();
    console.log("data id: " + fileIdPopover + ", fileName: " + fileNamePopover);
}

// callback function after getting answer from server
function handleFile(files) {
    console.log("handleFile ");
    var table = $("#filesTableBody");
    $("#files_table").show();

    //remove rows after previous click
    table.empty();

    for (var key in files) {
        if (files.hasOwnProperty(key)) {
            // do stuff

            console.log("add: " + files[key].name);
            var row =
                `<tr class="context_popup" data-toggle="popover" rel=context-popover id=${files[key].id} ondrop="drop(event)" ondragover="allowDrop(event)" draggable="true"
                    ondragstart="drag(event)" ">
                        <td> <a class="fileName" href="#">${files[key].name}</a></td>
                        <td>${files[key].type}</td>
                        <td>${getText(files[key].size)}</td>
                        <td>${getText(files[key].modified)}</td>
                    <td>
                    <a  href="#" class="hoverAble details_btn" data-toggle="popover" rel="popover" data-placement="left"
                    data-popover-content="#popoverContent"
                    data-trigger="focus">
                ...</a>
                    </td>
                    </tr>`;

            table.append($(row));

            console.log("id: " + files[key].id);
            var r = $(document.getElementById(files[key].id));
            r.on("dblclick", {id: files[key].id}, dblclickFile);
            r.on("click", {id: files[key].id}, rowClick);
            r.find("a.fileName").on("click", {id: files[key].id}, dblclickFile);
            var link = r.find('a');
            bindPopover();

            setContextMenuPopover(r);
            rowId = "";
        }
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

function setContextMenuPopover(r) {

    r.popover({
        placement: 'bottom',
        trigger: 'manual',
        html: true,
        content: detailsMenuContent
    });

    $(document).on('contextmenu', '.context_popup', function (event) {
        console.log("contextmenu ");
        // prevent default right click from browser
        event.preventDefault();

        fileIdPopover = this.id;
        fileNamePopover = $(document.getElementById(this.id)).find(".fileName").text();

        if (notEmpty(rowId) == 1 && rowId != this.id) {
            console.log("contextmenu remove popup for old id: " + rowId + ", new: " + this.id);
            // hide old
            $('.context_popup').popover('hide');
        }
        rowId = this.id;
        console.log("show ");
        $(this).popover('show');
    });


    //hide popover after left clicking any element
    $("body").click(function (e) {
        if (notEmpty(rowId)) {
            $('.context_popup').popover('hide');
        }
    });
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
    formData.append("filePath", filesProvider.fullPath);
    formData.append("cloudName", currentCloud);

    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8080/upload/', true);

    // Set up a handler for when the request finishes.
    xhr.onload = function () {
        if (xhr.status === 200) {
            // File(s) uploaded.
            console.log("UPLOADED! ");
            listFolder(currentCloud, filesProvider.fullPath);
        } else {
            alert('An error occurred!');
        }
    };
    // Send the Data.
    showTempAlert("Start uploading");
    xhr.send(formData);
}
