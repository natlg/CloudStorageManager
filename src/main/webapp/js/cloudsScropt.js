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

                for (var i = 0; i < cloudArray.length; i++) {
                    var cloud = ` <p><a href="#" id="${cloudArray[i].id}" onclick="cloudClick(event)" class="${cloudArray[i].cloudService}">${cloudArray[i].accountName}</a></p>`;
                    container.append($(cloud));
                }
            }


        }
        else {
            if (this.readyState === 4 && this.status === 401) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
                setAuthorized(false);
            }

        }
    };
    xhttp.open("POST", "http://localhost:8080/getclouds", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhttp.send();
}

function addCloud() {
    console.log("add_cloud click");
    var cloud = $("#cloud_select :selected").text();
    var cloudName = $("#cloud_name").val();
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
        "&cloudName=" + cloudName;
    xhttp.open("POST", "http://localhost:8080/addcloud", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
}


function cloudClick(event) {
    console.log("cloud click");
    console.log("id click: " + event.target.id);

    currentCloud = "Dropbox";
    listFolder("");
}
