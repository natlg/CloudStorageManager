var filesProvider;
var currentCloud;


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

$(document).ready(function () {
    console.log("document ready");
    filesProvider = new FilesProvider();
    getClouds();
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
    }

    $(document).on('click', '#login', function () {
        location.href = "login.html";
    });

    $(document).on('click', '#logout', logoutClick);

    $(document).on('click', '#add_cloud', addCloud);
});


