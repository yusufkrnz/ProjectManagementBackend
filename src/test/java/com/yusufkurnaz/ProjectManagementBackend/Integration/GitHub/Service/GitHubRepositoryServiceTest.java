package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.request.CreateRepositoryRequest;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.response.CreateRepositoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubRepositoryServiceTest {

    @Mock
    private GitHubRepositoryService gitHubRepositoryService;

    @Test
    void createRepository_ShouldReturnValidResponse() {
        // Given
        CreateRepositoryRequest request = new CreateRepositoryRequest();
        request.setInstallationId("123456");
        request.setAccountLogin("testuser");
        request.setName("test-repo");
        request.setDescription("Test repository");
        request.setPrivate(true);
        request.setInitializeReadme(true);

        CreateRepositoryResponse expectedResponse = new CreateRepositoryResponse(
                "https://github.com/testuser/test-repo",
                "https://github.com/testuser/test-repo.git"
        );

        when(gitHubRepositoryService.createRepository(request)).thenReturn(expectedResponse);

        // When
        CreateRepositoryResponse actualResponse = gitHubRepositoryService.createRepository(request);

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getHtmlUrl(), actualResponse.getHtmlUrl());
        assertEquals(expectedResponse.getCloneUrl(), actualResponse.getCloneUrl());
        verify(gitHubRepositoryService).createRepository(request);
    }

    @Test
    void createRepository_ShouldHandleNullRequest() {
        // Given
        when(gitHubRepositoryService.createRepository(null)).thenThrow(new IllegalArgumentException("Request cannot be null"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gitHubRepositoryService.createRepository(null);
        });
    }

    @Test
    void createRepository_ShouldValidateRequiredFields() {
        // Given
        CreateRepositoryRequest request = new CreateRepositoryRequest();
        request.setInstallationId("123456");
        // Missing required fields: accountLogin, name

        when(gitHubRepositoryService.createRepository(request))
                .thenThrow(new IllegalArgumentException("Account login and name are required"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            gitHubRepositoryService.createRepository(request);
        });
    }
}


