var filesProvider;
var currentCloud;



function isAuthorized() {
    var isAuthorized = sessionStorage.isAuthorized;
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
    if (typeof(Storage) !== "undefined") {
        sessionStorage.isAuthorized = auth;
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

$(document).ready(function () {
    filesProvider = new FilesProvider();
    console.log("document ready");

    getClouds();
    setHeader();

    $(document).on('click', '#login', function () {
        location.href = "login.html";
    });

    $(document).on('click', '#logout', logoutClick);

    $(document).on('click', '#Dropbox', function () {
        currentCloud = "Dropbox";
        listFolder("");
    });

    $(document).on('click', '#add_cloud', addCloud);

});


