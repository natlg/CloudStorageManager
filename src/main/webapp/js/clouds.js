function getClouds() {
    console.log("getClouds");
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);

            var responseText = xhttp.responseText;
            if (responseText === undefined || responseText == null || responseText.length <= 0) {
                console.log("got NULLL");
                setAuthorized(false);
            }
            else {
                setAuthorized(true);
                $("#table-icons-container").show();
                var userEmail = JSON.parse(xhttp.responseText).userEmail;
                var userFirstName = JSON.parse(xhttp.responseText).userFirstName;
                var userLastName = JSON.parse(xhttp.responseText).userLastName;
                console.log("user: " + userEmail + userFirstName + userLastName);
                var response = JSON.parse(xhttp.responseText).clouds;
                var arrayLength = response.length;
                var cloudArray = [];
                console.log("arrayLength: " + arrayLength);
                for (var i = 0; i < arrayLength; i++) {
                    var cloud = {
                        id: response[i].id,
                        accountName: response[i].accountName,
                        service: response[i].service
                    };
                    cloudArray.push(cloud);
                    console.log("push: " + cloudArray[i].accountName + " " + cloudArray[i].service);
                }

                var container = $("#cloud_container");
                container.empty();
                container.addClass("well");

                var addBtn = `
            <span data-toggle="modal" data-target="#myModal">
                    <a title="Add Cloud" class="icon-link" data-toggle="tooltip" >
                    <img class="icon" src="img/add-icon-2.png" alt="add cloud">
                    Add Cloud
                    </a>
                    </span>
                        <hr />`;

                container.append($(addBtn));

                filesProvider.clouds = cloudArray;

                for (var i = 0; i < cloudArray.length; i++) {
                    var cloudService = cloudArray[i].service;
                    var cloudIcon = ``;
                    switch (cloudService) {
                        case 'Dropbox':
                            cloudIcon = ` <img class="icon" src="img/dropbox-icon.png" alt="Dropbox">`;
                            break;
                        case 'OneDrive':
                            cloudIcon = ` <img class="icon" src="img/onedrive-icon.png" alt="OneDrive">`;
                            break;
                        case 'Google Drive':
                            cloudIcon = ` <img class="icon" src="img/googledrive-icon.png" alt="GoogleDrive">`;
                            break;
                    }

                    var cloud = `<li><div>`
                        + cloudIcon
                        + `<a href="#" id="${cloudArray[i].id}" onclick="cloudClick(event)" class="${cloudArray[i].service} cloud-padding">${cloudArray[i].accountName}</a></div></li>`;

                    console.log("cloud: " + cloud);

                    container.append($(cloud));
                }
            }
            loadAuthorizedPage();
        }
        else {
            if (this.readyState === 4) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
                setAuthorized(false);
                saveToSessionStorage("added_cloud_drive", "");
                loadAuthorizedPage();
            }
        }
    };
    xhttp.open("POST", domainName + "/getclouds", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhttp.send();
}


function addCloud() {
    console.log("add_cloud click");
    var cloudDrive = $("#cloud_select :selected").text();
    saveToSessionStorage("added_cloud_drive", cloudDrive);

    var cloudName = $("#cloud_name").val();
    saveToSessionStorage("added_cloud_name", cloudName);

    switch (cloudDrive) {
        case 'Dropbox':
            location.href = "https://www.dropbox.com/1/oauth2/authorize?client_id=Kg4d1ewybw95ovb&response_type=token&redirect_uri=" + domainName + "/index.html?redirect=dropbox";
            break;
        case 'OneDrive':
            location.href = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?" +
                "client_id=70a0893e-f51c-4f4b-abc0-827f347e4f43" +
                "&response_type=code" +
                "&redirect_uri=" + domainName + "/index.html" +
                "&response_mode=query" +
                "&scope=Files.ReadWrite.All%20offline_access";
            break;
        case 'Google Drive':
            location.href = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "scope=https://www.googleapis.com/auth/drive&" +
                "response_type=code&" +
                "access_type=offline&" +
                "include_granted_scopes=true&" +
                "state=state_parameter_passthrough_value&" +
                "redirect_uri=" + domainName + "/index.html&" +
                "client_id=770930937201-fr3kajpf35v7uelh5m120vn8dsqikno7.apps.googleusercontent.com";
            break;
    }
}

function addFolder() {
    console.log("addFolder click");
    var folderName = $("#folder_name").val();
    $("#folder_name").val("");
    var parentId = filesProvider.parentId;
    showTempAlert("Adding folder", 'info');

    var params = {
        folderName: folderName,
        parentId: parentId,
        path: filesProvider.fullPath,
        cloudName: currentCloud
    };
    callMethod("/addfolder", "POST", params, "Failed to add folder", function (response) {
        console.log("folder is added");
        listFolder(currentCloud, filesProvider.fullPath, filesProvider.parentId);
    });
}

function openInNewTab(url) {
    console.log("url: " + url);
    var win = window.open(url, '_blank');
    win.focus();
}

function getAuthorizeUrl() {
    console.log("getAuthorizeUrl");
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
            handleAuthorizeUrl(xhttp.responseText);
        }
        else {
            if (this.readyState === 4 && this.status === 401) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
                if (this.status == 401 || this.status == 400) {
                    console.log("go to login");
                    location.href = "login.html";
                }
            }
        }
    };
    xhttp.open("POST", domainName + "/getauthorizeurl", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhttp.send();
}

function handleAuthorizeUrl(authorizeUrl) {

    console.log("authorizeUrl: " + authorizeUrl);
    if (authorizeUrl) {
        openInNewTab(authorizeUrl);
    }
    var modalContainer = $("#modal-container");
    var addBtnContainer = $("#addBtnContainer");
    modalContainer.empty();
    addBtnContainer.empty();
    var codeElement = ` <div class="control-group">
                                <p>Code: </p>
                                <input id="cloud_code" type="text" class="form-control" name="cloud_code"
                                       placeholder="Cloud Code">
                            </div>`;
    modalContainer.append($(codeElement));

    var btnElement = ` <button id="add" type="button" class="btn btn-default" data-dismiss="modal">
                                Add Cloud
                            </button>`;
    addBtnContainer.append($(btnElement));
    $(document).on('click', '#add', function () {
        var code = $("#cloud_code").text();
        sendAddCloudRequest(cloud, cloudName, code);
    });
}

function sendAddCloudRequest(cloud, cloudName, code) {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
            getClouds();
        }
        else {
            if (this.readyState === 4 && this.status === 401) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
                if (this.status == 401 || this.status == 400) {
                    console.log("go to login");
                    location.href = "login.html";
                }
            }
        }
    };

    var params = "cloud=" + cloud +
        "&cloudName=" + cloudName + "&code=" + code;
    xhttp.open("POST", domainName + "/addcloud", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
}


function cloudClick(event) {
    console.log("cloud click");
    console.log("id click: " + event.target.id);
    $("#aboutPageText").hide();
    var clickedCloud = event.target.textContent;
    console.log("text click: " + clickedCloud);
    currentCloud = clickedCloud;
    filesProvider.pathIdList = [];
    listFolder(clickedCloud, "", "");
}
