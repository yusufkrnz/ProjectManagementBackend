package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InstallationSummary {
    private String installationId;
    private String accountLogin;
    private String accountType; // USER or ORG
}


