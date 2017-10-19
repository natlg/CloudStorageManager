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
                        cloudService: response[i].cloudService
                    };
                    cloudArray.push(cloud);
                    console.log("push: " + i);
                }

                var container = $("#cloud_container");
                container.empty();
                var addBtn = `  <p>
                <!-- Trigger the modal with a button -->
                <button type="button" class="btn btn-info btn-lg" data-toggle="modal" data-target="#myModal">+</button>
            </p>`;
                container.append($(addBtn));

                filesProvider.clouds = cloudArray;
                for (var i = 0; i < cloudArray.length; i++) {
                    var cloud = ` <p><a href="#" id="${cloudArray[i].id}" onclick="cloudClick(event)" class="${cloudArray[i].cloudService}">${cloudArray[i].accountName}</a></p>`;
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
    xhttp.open("POST", "http://localhost:8080/getclouds", true);
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
            location.href = "https://www.dropbox.com/1/oauth2/authorize?client_id=Kg4d1ewybw95ovb&response_type=token&redirect_uri=http://localhost:8080/indexpage.html?redirect=dropbox";

            break;
        case 'OneDrive':
            location.href = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?" +
                "client_id=70a0893e-f51c-4f4b-abc0-827f347e4f43" +
                "&response_type=code" +
                "&redirect_uri=http://localhost:8080/indexpage.html" +
                "&response_mode=query" +
                "&scope=Files.ReadWrite.All%20offline_access";
            break;
    }
}

function addFolder() {
    console.log("addFolder click");
    var folderName = $("#folder_name").val();
    $("#folder_name").val("");
    var parentId = filesProvider.parentId;
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
        }
        else {
            if (this.readyState === 4 && this.status === 401) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
            }
        }
    };
    var params = "folderName=" + folderName +
        "&cloudName=" + currentCloud + "&path=" + filesProvider.fullPath + "&parentId=" + parentId;
    xhttp.open("POST", "http://localhost:8080/addfolder", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
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
            }
        }
    };
    xhttp.open("POST", "http://localhost:8080/getauthorizeurl", true);
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
            }
        }
    };

    var params = "cloud=" + cloud +
        "&cloudName=" + cloudName + "&token=" + code;
    xhttp.open("POST", "http://localhost:8080/addcloud", true);
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
    listFolder(clickedCloud, "");
}
