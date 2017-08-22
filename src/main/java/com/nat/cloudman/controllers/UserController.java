package com.nat.cloudman.controllers;

import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.model.User;
import com.nat.cloudman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public FilesContainer logout(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("logout ");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return null;
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    @ResponseBody
    public String signUp(
            @RequestParam("email") String email,
            @RequestParam("firstname") String firstName,
            @RequestParam("lastname") String lastName,
            @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response

    ) {
        System.out.println("params. email: " + email + ", firstName: " + firstName + ", lastName: " + lastName + ", password: " + password);
        String result = "";
        User userExists = userService.findUserByEmail(email);
        if (userExists != null) {
            result = "User already exists";
        } else {
            User user = new User();
            user.setEmail(email);
            user.setName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            userService.saveUser(user);
            result = "User was saved";
        }
        System.out.println("return: " + result);
        System.out.println("from request: " + request.getParameter("email") + " " +
                request.getParameter("password"));
        return result;
    }

    @RequestMapping(value = "/loginform", method = RequestMethod.POST)
    @ResponseBody
    public String login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response

    ) {
        System.out.println("params. email: " + email + ", password: " + password);
        User userExists = userService.findUserByEmail(email);
        String result = "";
        if (userExists == null) {
            result = "User doesn't exist";
        } else {
            result = (userExists.getPassword().equals(password)) ? "login success" : "wrong password";
        }
        System.out.println("return for login: " + result);
        return result;
    }

}
