package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstallCallbackController.class)
class InstallCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handleInstallCallback_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/github/install/callback"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void handleInstallCallback_ShouldAcceptGetRequest() throws Exception {
        mockMvc.perform(get("/api/github/install/callback"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"));
    }
}


