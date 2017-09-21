package com.nat.cloudman.controllers;

import java.io.*;
import java.util.*;

import com.nat.cloudman.cloud.CloudManager;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.response.CloudContainer;
import com.nat.cloudman.response.DownloadedFileContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.model.User;
import com.nat.cloudman.service.CloudService;
import com.nat.cloudman.service.UserService;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @RequestMapping(value = "/listfiles", method = RequestMethod.POST)
    public FilesContainer listFiles(@RequestParam(value = "path", defaultValue = "") String path,
                                    @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
                                    HttpServletRequest request, HttpServletResponse response) {
        System.out.println("got path: " + path + ", cloudName: " + cloudName);

        userManager.showAuth("dropbox");
        return cloudManager.getFilesList(cloudName, path);
    }

    @RequestMapping(value = "/addfolder", method = RequestMethod.POST)
    public void addFolder(@RequestParam(value = "path", defaultValue = "") String path,
                          @RequestParam(value = "cloudName") String cloudName,
                          @RequestParam(value = "folderName") String folderName,
                          @RequestParam(value = "parentId") String parentId,
                          HttpServletRequest request, HttpServletResponse response) {
        System.out.println("folderName: " + folderName + ", path: " + path + ", cloudName: " + cloudName + ", parentId: " + parentId);
        cloudManager.addFolder(folderName, cloudName, path, parentId);
    }

    @RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam(value = "fileName", defaultValue = "") String fileName,
                                                            @RequestParam(value = "cloudName") String cloudName,
                                                            @RequestParam(value = "fileId") String fileId,
                                                            @RequestParam(value = "path") String path,
                                                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("download: fileName: " + fileName + ", fileId: " + fileId + ", cloudName: " + cloudName + ", path: " + path);
        return cloudManager.downloadFile(fileName, cloudName, fileId, path);
    }

    @RequestMapping(value = "/deletefile", method = RequestMethod.POST)
    public void deleteFile(@RequestParam(value = "fileName", defaultValue = "") String fileName,
                           @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
                           @RequestParam(value = "fileId", defaultValue = "") String fileId,
                           @RequestParam(value = "path", defaultValue = "") String path,
                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("deleteFile: fileName: " + fileName + ", fileId: " + fileId + ", cloudName: " + cloudName + ", path: " + path);
        cloudManager.deleteFile(fileName, cloudName, fileId, path);
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
            @RequestParam("filePath") String filePath,
            @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
            HttpServletRequest request, HttpServletResponse response
    ) {
        System.out.println("filePath: " + filePath + ", cloudName: " + cloudName);
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    System.out.println("file getOriginalFilename: " + file.getOriginalFilename());
                    System.out.println("file getContentType: " + file.getContentType());
                    System.out.println("file getName: " + file.getName());
                    System.out.println("file getSize: " + file.getSize());
                    File convertedFile = dropboxManager.multipartToFile(file, "E:\\pics\\uploaded\\");
                    System.out.println("convertedFile: " + convertedFile.exists() + " " + convertedFile.isFile() + " " + convertedFile.getName() + " " + convertedFile.getPath() + " " + convertedFile.getCanonicalPath());
                    cloudManager.uploadFile(cloudName, convertedFile, filePath + "/" + convertedFile.getName());
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                    e.printStackTrace();
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
