package com.nat.cloudstorage.controllers;

import com.nat.cloudstorage.response.FilesContainer;
import com.nat.cloudstorage.service.UserService;
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
        return userService.createAndSaveUser(email, firstName, lastName, password);
    }

    @RequestMapping(value = "/loginform", method = RequestMethod.POST)
    @ResponseBody
    public void login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response

    ) {
        System.out.println("loginform params: email: " + email + ", password: " + password);
    }

}
