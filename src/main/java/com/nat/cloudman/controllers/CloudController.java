package com.nat.cloudman.controllers;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.nat.cloudman.cloud.CloudManagerFacade;
import com.nat.cloudman.cloud.OneDriveManager;
import com.nat.cloudman.cloud.UserManager;
import com.nat.cloudman.controllers.params.CloudParameters;
import com.nat.cloudman.controllers.params.FileParameters;
import com.nat.cloudman.controllers.params.FolderParameters;
import com.nat.cloudman.controllers.params.TransitParameters;
import com.nat.cloudman.response.CloudContainer;
import com.nat.cloudman.response.FilesContainer;
import com.nat.cloudman.cloud.DropboxManager;
import com.nat.cloudman.model.Cloud;
import com.nat.cloudman.model.User;
import com.nat.cloudman.service.CloudService;
import com.nat.cloudman.service.UserService;
import com.nat.cloudman.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
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
    private UserManager userManager;


    @Autowired
    private CloudService cloudService;


    @Autowired
    CloudManagerFacade cloudManager;

    @Value("${temp.upload.path}")
    private String UPLOAD_PATH;

    @RequestMapping(value = "/listfiles", method = RequestMethod.POST)
    public FilesContainer listFiles(@RequestParam(value = "path", defaultValue = "") String path,
                                    @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
                                    HttpServletRequest request, HttpServletResponse response) {
        System.out.println(" listfiles got path: " + path + ", cloudName: " + cloudName);
        return cloudManager.getFilesList(cloudName, path);
    }

    @RequestMapping(value = "/addfolder", method = RequestMethod.POST)
    public void addFolder(@RequestBody FolderParameters params,
                          HttpServletRequest request, HttpServletResponse response) {
        System.out.println("addFolder: " + params);
        cloudManager.addFolder(params.folderName, params.cloudName, params.path, params.parentId);
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

    @RequestMapping(value = "/deletefile", method = RequestMethod.DELETE)
    public void deleteFile(@RequestBody FileParameters params,
                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("deleteFile " + params);
        cloudManager.deleteFile(params.cloudName, params.fileId, params.path);
    }

    @RequestMapping(value = "/renamefile", method = RequestMethod.POST)
    public void renameFile(@RequestBody FileParameters params,
                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("renameFile: " + params);
        cloudManager.renameFile(params.fileName, params.newName, params.cloudName, params.fileId, params.path);
    }


    @RequestMapping(value = "/getauthorizeurl", method = RequestMethod.POST)
    public String getAuthorizeUrl(HttpServletRequest request, HttpServletResponse response) {
        //TODO
        System.out.println("getauthorizeurl ");
        return null;
        //return dropboxManager.getAuthorizeUrl();
    }

    @RequestMapping(value = "/addcloud", method = RequestMethod.POST)
    public void addCloud(@RequestParam(value = "cloud", defaultValue = "") String cloudDrive,
                         @RequestParam(value = "cloudName", defaultValue = "") String cloudName,
                         @RequestParam(value = "code", defaultValue = "") String code,
                         HttpServletRequest request, HttpServletResponse response) {
        System.out.println("got cloud: " + cloudDrive + ", cloudName: " + cloudName + ", code: " + code);
        cloudService.addCloudToCurrentUser(cloudDrive, cloudName, code);
    }

    @RequestMapping(value = "/getclouds", method = RequestMethod.POST)
    public CloudContainer getClouds(
            HttpServletRequest request, HttpServletResponse response) {
        System.out.println("getclouds");
        User user = userManager.getUser();
        CloudContainer cloudContainer = new CloudContainer(user.getEmail(), user.getName(), user.getLastName());
        if (user != null) {
            Set<Cloud> clouds = user.getClouds();
            for (Cloud cl : clouds) {
                System.out.println("have cloud getAccountName: " + cl.getAccountName() + ",  getCloudService: " + cl.getCloudService());
                cloudContainer.addCloud(cl.getAccountName(), cl.getCloudService());
            }
            System.out.println("getClouds().size(): " + cloudContainer.getClouds().size());
            return cloudContainer;
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
                    File convertedFile = Utils.multipartToFile(file, UPLOAD_PATH);
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

    @RequestMapping(value = "/getcloudstree", method = RequestMethod.POST)
    public FilesContainer getCloudsTree(
            @RequestBody CloudParameters params,
            HttpServletRequest request, HttpServletResponse response) {
        String cloudName = params.cloudName;
        String path = params.path;
        String id = params.id;
        System.out.println("getcloudstree, cloudName: " + cloudName + ", path: " + path + ", id: " + id);
        User user = userManager.getUser();
        Set<Cloud> clouds = user.getClouds();
        for (Cloud cl : clouds) {
            if (cl.getAccountName().equals(cloudName)) {
                System.out.println("have cloud getAccountName: " + cl.getAccountName());
                System.out.println("have cloud getCloudService: " + cl.getCloudService());
                FilesContainer f = cloudManager.getFilesList(cl.getAccountName(), path);
                return f;
            }
        }
        return null;
    }

    @RequestMapping(value = "/copy", method = RequestMethod.POST)
    public void copyFile(
            @RequestBody TransitParameters params,
            HttpServletRequest request, HttpServletResponse response) {
        System.out.println("copy, " + params);
        cloudManager.copyFile(params.cloudSource, params.pathSource, params.idSource, params.downloadUrl, params.cloudDest, params.pathDest, params.idDest);
    }

    @RequestMapping(value = "/move", method = RequestMethod.POST)
    public void moveFile(
            @RequestBody TransitParameters params,
            HttpServletRequest request, HttpServletResponse response) {
        System.out.println("move, " + params);
        cloudManager.moveFile(params.cloudSource, params.pathSource, params.idSource, params.downloadUrl, params.cloudDest, params.pathDest, params.idDest);
    }

    @RequestMapping(value = "/getthumbnail", method = RequestMethod.POST)
    public String getThumbnail(
            @RequestBody FileParameters params,
            HttpServletRequest request, HttpServletResponse response) {
        System.out.println("getthumbnail, " + params);
        return cloudManager.getThumbnail(params);
    }

    @RequestMapping(value = "/removecloud", method = RequestMethod.DELETE)
    public void removeCloud(@RequestBody CloudParameters paramRemoveCloud) {
        System.out.println("remove cloud: " + paramRemoveCloud.cloudName);
        cloudService.removeCloud(paramRemoveCloud.cloudName);
    }

    private void addCorsHeader(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type,X-XSRF-TOKEN");
        response.addHeader("Access-Control-Max-Age", "1");
    }
}
