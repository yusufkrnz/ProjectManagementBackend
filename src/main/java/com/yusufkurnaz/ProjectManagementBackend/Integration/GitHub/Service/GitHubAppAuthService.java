package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

public interface GitHubAppAuthService {
    String createAppJwt();
    String createInstallationAccessToken(String installationId);
}


