package com.nat.cloudstorage.controllers;

import com.nat.cloudstorage.cloud.transfer.TransferTask;
import com.nat.cloudstorage.response.FilesContainer;
import com.nat.cloudstorage.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public FilesContainer logout(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("logout ");
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
        logger.debug("signUp");
        return userService.createAndSaveUser(email, firstName, lastName, password);
    }

    @RequestMapping(value = "/loginform", method = RequestMethod.POST)
    @ResponseBody
    public void login(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletRequest request, HttpServletResponse response

    ) {
        logger.debug("login");
    }

}
