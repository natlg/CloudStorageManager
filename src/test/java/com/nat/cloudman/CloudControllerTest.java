package com.nat.cloudman;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonpath.JsonPath;
import com.nat.cloudman.controllers.params.FileParameters;
import com.nat.cloudman.controllers.params.FolderParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class CloudControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(CloudControllerTest.class);

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private String oneDrivaCloudName = "OD cloud";
    private String oneDriveFolderName = "od folder test";
    private String oneDriveRootId = "3126D7302C73EA98!101";

    private String dropboxCloudName = "Dropbox cloud";
    private String dropboxFolderName = "dropbox folder test";
    private String dropboxRootId = "";

    private String googleCloudName = "GOOGLE!!";
    private String googleFolderName = "google folder test";
    private String googleRootId = "0AOMJr8Ji_BXuUk9PVA";

    @WithMockUser(username = "cltest5@outlook.com", roles = {"ADMIN"})
    @Before
    public void setup() throws Exception {
        logger.debug("test setup");
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @WithMockUser(username = "cltest5@outlook.com", roles = {"ADMIN"})
    @Test
    public void listFiles() throws Exception {
        logger.debug("test listFiles");
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        MvcResult result = mockMvc.perform(post("/listfiles")
                .param("path", "")
                .param("folderId", "")
                .param("cloudName", "Dropbox cloud"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        logger.debug("content: " + content + ", status: " + result.getResponse().getStatus());
    }

    public Map<String, Object> filePresents(String path, String folderId, String cloudName, String fileName, String fileType) throws Exception {
        logger.debug("filePresents, path: " + path + ", folderId: " + folderId +
                ", cloudName: " + cloudName + ", fileName: " + fileName + ", fileType: " + fileType);
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        MvcResult result = mockMvc.perform(post("/listfiles")
                .param("path", path)
                .param("folderId", folderId)
                .param("cloudName", cloudName))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        logger.debug("content: " + content + ", status: " + result.getResponse().getStatus());

        List<Map<String, Object>> dataList = JsonPath.parse(content)
                .read("$.files");
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> file = dataList.get(i);
            String name = (String) file.get("name");
            String type = (String) file.get("type");
            logger.debug("i: " + i + ", name: " + name + ", type: " + type);
            if (name.equals(fileName) && type.equals(fileType)) {
                return file;
            }
        }
        return null;
    }

    public boolean removeFile(String fileId, String path, String cloudName, String parentId, String fileName) throws Exception {
        logger.debug("removeFile: fileId: " + fileId + ", path: " + path + ", cloudName: " + cloudName + ", parentId: " + parentId);
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        FileParameters params = new FileParameters(fileName, fileId, cloudName, path, "", parentId);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(params);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/deletefile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        logger.debug("content: " + content + ", status: " + result.getResponse().getStatus());
        if (result.getResponse().getStatus() == 200) {
            return true;
        }
        return false;
    }

    @WithMockUser(username = "cltest5@outlook.com", roles = {"ADMIN"})
    @Test
    public void uploadFileTest() throws Exception {

        ////////-----------------   dropbox
        logger.debug("uploadFileTest");
        // params are in request body
        FolderParameters params = new FolderParameters("", dropboxCloudName, dropboxFolderName, "");
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(params);
        logger.debug("params: " + json);
        MvcResult result = mockMvc.perform(post("http://localhost:8080/addfolder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        logger.debug("dropbox content: " + content + ", status: " + result.getResponse().getStatus());
        Map<String, Object> file = filePresents("", "", dropboxCloudName, dropboxFolderName, "folder");
        assertNotNull(file);
        assertTrue(uploadSampleFile((String) file.get("pathLower"), (String) file.get("id"), dropboxCloudName));

        Map<String, Object> uploadedFile = filePresents((String) file.get("pathLower"), (String) file.get("id"), dropboxCloudName, "test.jpg", "file");
        assertNotNull(uploadedFile);
        assertEquals("test.jpg", (String) uploadedFile.get("name"));
        assertTrue(removeFile((String) file.get("id"), (String) file.get("pathLower"), dropboxCloudName, dropboxRootId, "test.jpg"));

        //////------------------  google----------------
        params = new FolderParameters("", googleCloudName, googleFolderName, googleRootId);
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        json = ow.writeValueAsString(params);
        logger.debug("params: " + json);
        result = mockMvc.perform(post("/addfolder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        logger.debug("google content: " + content + ", status: " + result.getResponse().getStatus());
        file = filePresents("", googleRootId, googleCloudName, googleFolderName, "folder");
        assertNotNull(file);
        assertTrue(uploadSampleFile((String) file.get("pathLower"), (String) file.get("id"), googleCloudName));

        uploadedFile = filePresents((String) file.get("pathLower"), (String) file.get("id"), googleCloudName, "test.jpg", "file");
        assertNotNull(uploadedFile);
        assertEquals("test.jpg", (String) uploadedFile.get("name"));
        assertTrue(removeFile((String) file.get("id"), (String) file.get("pathLower"), googleCloudName, googleRootId, "test.jpg"));


        ////////------------ onedrive-----------
        params = new FolderParameters("", oneDrivaCloudName, oneDriveFolderName, oneDriveRootId);
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        json = ow.writeValueAsString(params);
        logger.debug("params: " + json);
        result = mockMvc.perform(post("/addfolder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        logger.debug("oneDriv content: " + content + ", status: " + result.getResponse().getStatus());
        file = filePresents("", oneDriveRootId, oneDrivaCloudName, oneDriveFolderName, "folder");
        assertNotNull(file);
        assertTrue(uploadSampleFile((String) file.get("pathLower"), (String) file.get("id"), oneDrivaCloudName));
        uploadedFile = filePresents((String) file.get("pathLower"), (String) file.get("id"), oneDrivaCloudName, "test.jpg", "file");
        assertNotNull(uploadedFile);
        assertEquals("test.jpg", (String) uploadedFile.get("name"));
        assertTrue(removeFile((String) file.get("id"), (String) file.get("pathLower"), oneDrivaCloudName, oneDriveRootId, "test.jpg"));
    }


    public boolean uploadSampleFile(String filePath, String parentId, String cloudName) throws Exception {
        logger.debug("uploadSampleFile filePath: " + filePath + ", parentId: " + parentId + ", cloudName: " + cloudName);
        if (filePath == null) {
            filePath = "";
        }
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        FileInputStream fis = new FileInputStream("C:\\pics\\test.jpg");
        // !!! "files" is parameter name for files array
        MockMultipartFile multipartFile = new MockMultipartFile("files", "test.jpg", "image/jpeg", fis);
        // to sent few files
        // MockMultipartFile multipartFile2 = new MockMultipartFile("files", "file2.txt", "text/plain", "This document intended to be uploaded and attached to thread.".getBytes());
        logger.debug("multipartFile name: " + multipartFile.getName() + ", size: " + multipartFile.getSize());
        MvcResult result = mockMvc
                .perform
                        (MockMvcRequestBuilders.fileUpload("http://localhost:8080/upload")
                                .file(multipartFile)
                                // .file(multipartFile2)
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .param("filePath", filePath)
                                .param("parentId", parentId)
                                .param("cloudName", cloudName))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        logger.debug("content uploadSampleFile: " + content + ", status: " + result.getResponse().getStatus());
        if (result.getResponse().getStatus() == 200) {
            return true;
        }
        return false;
    }

    @WithMockUser(username = "cltest5@outlook.com", roles = {"ADMIN"})
    @Test
    public void addFolder() throws Exception {

        ////////-----------------   dropbox
        logger.debug("test addFolder");
        // params are in request body
        FolderParameters params = new FolderParameters("", dropboxCloudName, dropboxFolderName, "");
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(params);
        logger.debug("params: " + json);
        MvcResult result = mockMvc.perform(post("http://localhost:8080/addfolder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        logger.debug("dropbox content: " + content + ", status: " + result.getResponse().getStatus());
        Map<String, Object> file = filePresents("", "", dropboxCloudName, dropboxFolderName, "folder");
        assertNotNull(file);
        assertTrue(removeFile((String) file.get("id"), (String) file.get("pathLower"), dropboxCloudName, dropboxRootId, dropboxFolderName));

        ////////------------------  google----------------
        params = new FolderParameters("", googleCloudName, googleFolderName, googleRootId);
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        json = ow.writeValueAsString(params);
        logger.debug("params: " + json);
        result = mockMvc.perform(post("/addfolder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        logger.debug("google content: " + content + ", status: " + result.getResponse().getStatus());
        file = filePresents("", googleRootId, googleCloudName, googleFolderName, "folder");
        assertNotNull(file);
        assertTrue(removeFile((String) file.get("id"), (String) file.get("pathLower"), googleCloudName, googleRootId, googleFolderName));

        ////////------------ onedrive-----------
        params = new FolderParameters("", oneDrivaCloudName, oneDriveFolderName, oneDriveRootId);
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        json = ow.writeValueAsString(params);
        logger.debug("params: " + json);
        result = mockMvc.perform(post("/addfolder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn();
        content = result.getResponse().getContentAsString();
        logger.debug("oneDriv content: " + content + ", status: " + result.getResponse().getStatus());
        file = filePresents("", oneDriveRootId, oneDrivaCloudName, oneDriveFolderName, "folder");
        assertNotNull(file);
        assertTrue(removeFile((String) file.get("id"), (String) file.get("pathLower"), oneDrivaCloudName, oneDriveRootId, googleFolderName));
    }

    @WithMockUser(username = "cltest5@outlook.com", roles = {"ADMIN"})
    @Test
    public void getClouds() throws Exception {
        logger.debug("test getClouds");
        mockMvc.perform(post("/getclouds"
        ))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.clouds", hasSize(3)))
                .andExpect(jsonPath("$.clouds[0].accountName", is("Dropbox cloud")))
                .andExpect(jsonPath("$.clouds[0].service", is("Dropbox")));
    }

    @After
    public void cleanUp() throws Exception {
    }
}
