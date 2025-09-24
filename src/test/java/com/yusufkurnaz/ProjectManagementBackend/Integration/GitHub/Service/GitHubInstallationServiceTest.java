package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Dto.InstallationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubInstallationServiceTest {

    @Mock
    private GitHubInstallationService gitHubInstallationService;

    @Test
    void listInstallationsForUser_ShouldReturnInstallationList() {
        // Given
        String userId = "user123";
        List<InstallationSummary> expectedInstallations = Arrays.asList(
                new InstallationSummary("123456", "testuser", "USER"),
                new InstallationSummary("789012", "testorg", "ORG")
        );

        when(gitHubInstallationService.listInstallationsForUser(userId)).thenReturn(expectedInstallations);

        // When
        List<InstallationSummary> actualInstallations = gitHubInstallationService.listInstallationsForUser(userId);

        // Then
        assertNotNull(actualInstallations);
        assertEquals(2, actualInstallations.size());
        assertEquals("123456", actualInstallations.get(0).getInstallationId());
        assertEquals("testuser", actualInstallations.get(0).getAccountLogin());
        assertEquals("USER", actualInstallations.get(0).getAccountType());
        verify(gitHubInstallationService).listInstallationsForUser(userId);
    }

    @Test
    void listInstallationsForUser_ShouldReturnEmptyListWhenNoInstallations() {
        // Given
        String userId = "user123";
        when(gitHubInstallationService.listInstallationsForUser(userId)).thenReturn(Collections.emptyList());

        // When
        List<InstallationSummary> actualInstallations = gitHubInstallationService.listInstallationsForUser(userId);

        // Then
        assertNotNull(actualInstallations);
        assertTrue(actualInstallations.isEmpty());
        verify(gitHubInstallationService).listInstallationsForUser(userId);
    }

    @Test
    void listInstallationsForUser_ShouldHandleNullUserId() {
        // Given
        when(gitHubInstallationService.listInstallationsForUser(null))
                .thenThrow(new IllegalArgumentException("User ID cannot be null"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gitHubInstallationService.listInstallationsForUser(null);
        });
    }

    @Test
    void listInstallationsForUser_ShouldHandleEmptyUserId() {
        // Given
        when(gitHubInstallationService.listInstallationsForUser(""))
                .thenThrow(new IllegalArgumentException("User ID cannot be empty"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gitHubInstallationService.listInstallationsForUser("");
        });
    }
}


