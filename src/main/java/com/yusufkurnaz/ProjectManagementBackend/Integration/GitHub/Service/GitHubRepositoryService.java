package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.request.CreateRepositoryRequest;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.response.CreateRepositoryResponse;

public interface GitHubRepositoryService {
    CreateRepositoryResponse createRepository(CreateRepositoryRequest request);
}


