package com.nat.cloudstorage.cloud;

import com.nat.cloudstorage.controllers.UserController;
import com.nat.cloudstorage.model.Cloud;
import com.nat.cloudstorage.model.User;
import com.nat.cloudstorage.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserManager {

    @Autowired
    private UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    public User getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            logger.debug("user name: " + auth.getName() + " isAuthenticated: " + auth.isAuthenticated());
            User user = userService.findUserByEmail(auth.getName());
            return user;
        }
        logger.debug("auth is null");
        return null;
    }

    public Cloud getCloud(String cloudName) {
        Set<Cloud> clouds = getUser().getClouds();
        for (Cloud cloud : clouds) {
            if (cloud.getAccountName().equals(cloudName)) {
                return cloud;
            }
        }
        return null;
    }

    public void showAuth(String path) {
        logger.debug("auth in path: " + path);
        User user = getUser();
        if (user != null) {
            logger.debug("User name: " + user.getName());
            logger.debug("User email: " + user.getEmail());
            logger.debug("User id: " + user.getId());
        } else {
            logger.debug("User is null ");
        }
    }
}
