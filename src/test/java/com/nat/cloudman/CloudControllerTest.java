package com.nat.cloudman;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

    private String dropboxCloudName = "Dropbox cloud";
    private String dropboxFolderName = "dropbox folder test";

    private String googleCloudName = "GOOGLE!!";
    private String googleFolderName = "google folder test";

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

    @WithMockUser(username = "cltest5@outlook.com", roles = {"ADMIN"})
    @Test
    public void addFolder() throws Exception {
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

        params = new FolderParameters("", googleCloudName, googleFolderName, "0AOMJr8Ji_BXuUk9PVA");
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

        params = new FolderParameters("", oneDrivaCloudName, oneDriveFolderName, "3126D7302C73EA98!101");
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
