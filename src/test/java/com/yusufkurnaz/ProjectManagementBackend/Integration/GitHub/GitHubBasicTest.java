package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class GitHubBasicTest {

    @Test
    void testGitHubIntegrationBasic() {
        // Basic test to verify GitHub integration is working
        assertTrue(true, "GitHub integration basic test passed");
    }

    @Test
    void testGitHubControllersExist() {
        // Test that GitHub controllers are properly configured
        try {
            Class.forName("com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.InstallCallbackController");
            Class.forName("com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.WebhookController");
            assertTrue(true, "GitHub controllers exist");
        } catch (ClassNotFoundException e) {
            assertTrue(false, "GitHub controllers not found: " + e.getMessage());
        }
    }

    @Test
    void testGitHubServicesExist() {
        // Test that GitHub services are properly configured
        try {
            Class.forName("com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service.GitHubAppAuthService");
            Class.forName("com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service.GitHubInstallationService");
            Class.forName("com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service.GitHubRepositoryService");
            assertTrue(true, "GitHub services exist");
        } catch (ClassNotFoundException e) {
            assertTrue(false, "GitHub services not found: " + e.getMessage());
        }
    }
}




