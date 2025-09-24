package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

public interface GitHubAppAuthService {
    default String createAppJwt() {
        return null;
    }

    String createInstallationAccessToken(String installationId);
}


