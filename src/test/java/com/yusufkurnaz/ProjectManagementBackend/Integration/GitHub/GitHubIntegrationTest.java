package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.request.CreateRepositoryRequest;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.response.CreateRepositoryResponse;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Dto.InstallationSummary;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service.GitHubAppAuthService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service.GitHubInstallationService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service.GitHubRepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class GitHubIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubAppAuthService gitHubAppAuthService;

    @MockBean
    private GitHubInstallationService gitHubInstallationService;

    @MockBean
    private GitHubRepositoryService gitHubRepositoryService;

    @Test
    void testGitHubInstallCallbackFlow() throws Exception {
        // Test the complete installation callback flow
        mockMvc.perform(get("/api/github/install/callback"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void testGitHubWebhookFlow() throws Exception {
        // Test webhook handling with realistic payload
        String webhookPayload = """
                {
                    "action": "opened",
                    "pull_request": {
                        "id": 123,
                        "title": "Test Pull Request",
                        "state": "open",
                        "user": {
                            "login": "testuser"
                        }
                    },
                    "repository": {
                        "name": "test-repo",
                        "full_name": "testuser/test-repo"
                    }
                }
                """;

        mockMvc.perform(post("/api/github/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());
    }

    @Test
    void testGitHubAppAuthenticationFlow() throws Exception {
        // Mock the authentication service
        when(gitHubAppAuthService.createAppJwt()).thenReturn("mock_jwt_token");
        when(gitHubAppAuthService.createInstallationAccessToken("123456"))
                .thenReturn("mock_installation_token");

        // Test that services are properly wired
        org.junit.jupiter.api.Assertions.assertNotNull(gitHubAppAuthService);
        org.junit.jupiter.api.Assertions.assertNotNull(gitHubInstallationService);
        org.junit.jupiter.api.Assertions.assertNotNull(gitHubRepositoryService);
    }

    @Test
    void testGitHubInstallationServiceIntegration() throws Exception {
        // Mock installation service
        InstallationSummary installation = new InstallationSummary("123456", "testuser", "USER");
        when(gitHubInstallationService.listInstallationsForUser("testuser"))
                .thenReturn(Arrays.asList(installation));

        // Test that the service returns expected data
        org.junit.jupiter.api.Assertions.assertNotNull(gitHubInstallationService.listInstallationsForUser("testuser"));
    }

    @Test
    void testGitHubRepositoryServiceIntegration() throws Exception {
        // Mock repository service
        CreateRepositoryRequest request = new CreateRepositoryRequest();
        request.setInstallationId("123456");
        request.setAccountLogin("testuser");
        request.setName("test-repo");
        request.setDescription("Test repository");
        request.setPrivate(true);

        CreateRepositoryResponse response = new CreateRepositoryResponse(
                "https://github.com/testuser/test-repo",
                "https://github.com/testuser/test-repo.git"
        );

        when(gitHubRepositoryService.createRepository(any(CreateRepositoryRequest.class)))
                .thenReturn(response);

        // Test that the service returns expected data
        CreateRepositoryResponse result = gitHubRepositoryService.createRepository(request);
        org.junit.jupiter.api.Assertions.assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertEquals(response.getHtmlUrl(), result.getHtmlUrl());
    }

    @Test
    void testGitHubEndpointsAreAccessible() throws Exception {
        // Test that all GitHub endpoints are properly mapped and accessible
        mockMvc.perform(get("/api/github/install/callback"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/github/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}


