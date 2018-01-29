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
    xhttp.open("POST", "/logout/", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    console.log("send logout");
    xhttp.send();
}

function submitLoginForm() {
    console.log("Submit login form");
    var form = document.getElementById("loginform");
    // Initialize form validation on the login form.
    $("#loginform").validate({
        // Specify validation rules
        rules: {
            email: {
                required: true,
                // Specify that email should be validated
                // by the built-in "email" rule
                email: true
            },
            password: {
                required: true,
                minlength: 5
            }
        },
        // Specify validation error messages
        messages: {
            password: {
                required: "Please provide a password",
                minlength: "Your password must be at least 5 characters long"
            },
            email: "Please enter a valid email address"
        },
        submitHandler: function (form) {
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
            xhttp.open("POST", "/loginform", true);
            xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            console.log("send params:");
            console.log(params);
            xhttp.send(params);
        }
    });
}

function submitSignupForm() {
    console.log("Submit sign up form");
    var form = document.getElementById("signupform");
    $("#signupform").validate({
        // Specify validation rules
        rules: {
            firstname: "required",
            lastname: "required",
            email: {
                required: true,
                // Specify that email should be validated
                // by the built-in "email" rule
                email: true
            },
            password: {
                required: true,
                minlength: 5
            }
        },
        // Specify validation error messages
        messages: {
            firstname: "Please enter your firstname",
            lastname: "Please enter your lastname",
            password: {
                required: "Please provide a password",
                minlength: "Your password must be at least 5 characters long"
            },
            email: "Please enter a valid email address"
        },
        submitHandler: function (form) {
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
            xhttp.open("POST", "/signup", true);
            xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            console.log("send params:");
            console.log(params);
            xhttp.send(params);
        }
    });

}