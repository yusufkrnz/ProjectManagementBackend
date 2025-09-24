package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubAppAuthServiceTest {

    @Mock
    private GitHubAppAuthService gitHubAppAuthService;

    @Test
    void createAppJwt_ShouldReturnValidJwtToken() {
        // Given
        String expectedJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiIxMjM0NTY3ODkwIiwic3ViIjoiYXBwIn0.test";
        when(gitHubAppAuthService.createAppJwt()).thenReturn(expectedJwt);

        // When
        String actualJwt = gitHubAppAuthService.createAppJwt();

        // Then
        assertNotNull(actualJwt);
        assertTrue(actualJwt.startsWith("eyJ"));
        assertEquals(expectedJwt, actualJwt);
        verify(gitHubAppAuthService).createAppJwt();
    }

    @Test
    void createInstallationAccessToken_ShouldReturnValidToken() {
        // Given
        String installationId = "123456";
        String expectedToken = "ghs_test_token_123456789";
        when(gitHubAppAuthService.createInstallationAccessToken(installationId)).thenReturn(expectedToken);

        // When
        String actualToken = gitHubAppAuthService.createInstallationAccessToken(installationId);

        // Then
        assertNotNull(actualToken);
        assertTrue(actualToken.startsWith("ghs_"));
        assertEquals(expectedToken, actualToken);
        verify(gitHubAppAuthService).createInstallationAccessToken(installationId);
    }

    @Test
    void createInstallationAccessToken_ShouldHandleNullInstallationId() {
        // Given
        when(gitHubAppAuthService.createInstallationAccessToken(null))
                .thenThrow(new IllegalArgumentException("Installation ID cannot be null"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gitHubAppAuthService.createInstallationAccessToken(null);
        });
    }

    @Test
    void createInstallationAccessToken_ShouldHandleEmptyInstallationId() {
        // Given
        when(gitHubAppAuthService.createInstallationAccessToken(""))
                .thenThrow(new IllegalArgumentException("Installation ID cannot be empty"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gitHubAppAuthService.createInstallationAccessToken("");
        });
    }

    @Test
    void createAppJwt_ShouldHandleAuthFailure() {
        // Given
        when(gitHubAppAuthService.createAppJwt())
                .thenThrow(new RuntimeException("Failed to create JWT token"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            gitHubAppAuthService.createAppJwt();
        });
    }
}


