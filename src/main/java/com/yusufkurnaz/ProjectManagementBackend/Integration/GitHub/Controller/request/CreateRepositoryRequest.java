package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.request;

import lombok.Data;

@Data
public class CreateRepositoryRequest {
    private String installationId;
    private String accountLogin;
    private String name;
    private String description;
    private boolean isPrivate;
    private boolean initializeReadme;
    private String gitignoreTemplate;
    private String licenseTemplate;
}


