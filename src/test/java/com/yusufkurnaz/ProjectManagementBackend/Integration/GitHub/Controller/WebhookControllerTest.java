package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void receiveWebhook_ShouldAcceptPostRequest() throws Exception {
        String webhookPayload = """
                {
                    "action": "opened",
                    "pull_request": {
                        "id": 123,
                        "title": "Test PR",
                        "state": "open"
                    }
                }
                """;

        mockMvc.perform(post("/api/github/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());
    }

    @Test
    void receiveWebhook_ShouldHandleEmptyPayload() throws Exception {
        mockMvc.perform(post("/api/github/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void receiveWebhook_ShouldHandleInvalidJson() throws Exception {
        mockMvc.perform(post("/api/github/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isBadRequest());
    }
}


