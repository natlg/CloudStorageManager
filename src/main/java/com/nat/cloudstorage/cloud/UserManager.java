package com.nat.cloudstorage.cloud;

import com.nat.cloudstorage.model.Cloud;
import com.nat.cloudstorage.model.User;
import com.nat.cloudstorage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserManager {

    @Autowired
    private UserService userService;

    public User getUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            System.out.println("user name: " + auth.getName() + " isAuthenticated: " + auth.isAuthenticated());
            User user = userService.findUserByEmail(auth.getName());
            return user;
        }
        System.out.println("auth is null");
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
        System.out.println("auth in path: " + path);
        User user = getUser();
        if (user != null) {
            System.out.println("User name: " + user.getName());
            System.out.println("User email: " + user.getEmail());
            System.out.println("User id: " + user.getId());
        } else {
            System.out.println("User is null ");
        }
    }
}
