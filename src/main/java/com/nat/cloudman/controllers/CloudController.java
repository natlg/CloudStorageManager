package com.nat.cloudman.controllers;

import java.io.*;
import java.util.*;

import com.nat.cloudman.cloud.CloudManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.response.CloudContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.model.User;
import com.nat.cloudman.service.CloudService;
import com.nat.cloudman.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//access from any domain
//@CrossOrigin(origins = "*")
@RestController
public class CloudController {

    @Autowired
    private UserService userService;
    @Autowired
    private DropboxManager dropboxManager;

    @Autowired
    private UserManager userManager;


    @Autowired
    private CloudService cloudService;

    @Autowired
    OneDriveManager oneDriveManager;

    @Autowired
    CloudManager cloudManager;

    @RequestMapping(value = "/dropbox", method = RequestMethod.POST)
    public FilesContainer listFiles(@RequestParam(value = "path", defaultValue = "") String path,
                                    @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
                                    HttpServletRequest request, HttpServletResponse response) {
        System.out.println("got path: " + path + ", cloudName: " + cloudName);

        userManager.showAuth("dropbox");
        return new FilesContainer(cloudManager.getFilesList(cloudName, path));
    }

    @RequestMapping(value = "/getauthorizeurl", method = RequestMethod.POST)
    public String getAuthorizeUrl(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("getauthorizeurl ");
        return dropboxManager.getAuthorizeUrl();
    }

    @RequestMapping(value = "/addcloud", method = RequestMethod.POST)
    public void addCloud(@RequestParam(value = "cloud", defaultValue = "") String cloudDrive,
                         @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
                         @RequestParam(value = "token", defaultValue = "") String token,
                         HttpServletRequest request, HttpServletResponse response) {
        System.out.println("got cloud: " + cloudDrive + ", cloudName: " + cloudName + ", token: " + token);
        cloudService.addCloudToCurrentUser(cloudDrive, cloudName, token);
    }

    @RequestMapping(value = "/getclouds", method = RequestMethod.POST)
    public CloudContainer getClouds(
            HttpServletRequest request, HttpServletResponse response) {
        System.out.println("getclouds");
        User user = userManager.getUser();
        if (user != null) {
            Set<Cloud> clouds = user.getClouds();
            for (Cloud cl : clouds) {
                System.out.println("have cloud getAccountName: " + cl.getAccountName());
                System.out.println("have cloud getCloudService: " + cl.getCloudService());
            }
            return new CloudContainer(clouds, user.getEmail(), user.getName(), user.getLastName());
        }
        return null;
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public String handleFileUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("dropboxPath") String dropboxPath,
            @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
            HttpServletRequest request, HttpServletResponse response
    ) {
        System.out.println("dropboxPath: " + dropboxPath + ", cloudName: " + cloudName);
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    System.out.println("file getOriginalFilename: " + file.getOriginalFilename());
                    System.out.println("file getContentType: " + file.getContentType());
                    System.out.println("file getName: " + file.getName());
                    System.out.println("file getSize: " + file.getSize());
                    File convertedFile = dropboxManager.multipartToFile(file, "E:\\pics\\uploaded\\");
                    System.out.println("convertedFile: " + convertedFile.exists() + " " + convertedFile.isFile() + " " + convertedFile.getName() + " " + convertedFile.getPath() + " " + convertedFile.getCanonicalPath());
                    dropboxManager.uploadFile(cloudName, convertedFile, dropboxPath + "/" + convertedFile.getName());
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                }
            } else {
                System.out.println("file is empty ");
            }
        }
        return null;
    }

    private void addCorsHeader(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type,X-XSRF-TOKEN");
        response.addHeader("Access-Control-Max-Age", "1");
    }

}
