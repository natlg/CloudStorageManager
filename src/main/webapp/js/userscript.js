function logoutClick() {
    console.log("logoutClick");
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
            location.href = "indexpage.html";
            setAuthorized(false);
            $("#table-icons-container").hide();
        }
        else {
            console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
        }
    };
    xhttp.open("POST", "http://localhost:8080/logout/", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send logout");
    xhttp.send();
}

function submitLoginForm() {
    console.log("Submit login form");
    var form = document.getElementById("loginform");
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
            location.href = "indexpage.html";
            setAuthorized(true);
            console.log("XMLHttpRequest isLogged: " + isAuthorized());
        }
        else {
            if (this.readyState === 4 && this.status === 401) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
                $('#login-alert').show();
            }

        }
    };

    var params = "email=" + form.elements["email"].value +
        "&password=" + form.elements["password"].value;
    xhttp.open("POST", "http://localhost:8080/loginform", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
}

function submitSignupForm() {
    console.log("Submit sign up form");
    var form = document.getElementById("signupform");
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            console.log("XMLHttpRequest answer is ready");
            console.log("responseText: " + xhttp.responseText);
            location.href = "login.html";
        }
        else {
            if (this.readyState === 4 && this.status === 401) {
                console.log("error in XMLHttpRequest, status: " + this.status, ", readyState: " + this.readyState);
                $("#signup-alert").show();
            }
        }
    };

    var params = "email=" + form.elements["email"].value +
        "&firstname=" + form.elements["firstname"].value +
        "&lastname=" + form.elements["lastname"].value +
        "&password=" + form.elements["password"].value;
    xhttp.open("POST", "http://localhost:8080/signup", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send params:");
    console.log(params);
    xhttp.send(params);
}