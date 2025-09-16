package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Service;

import java.util.List;
import com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Dto.InstallationSummary;

public interface GitHubInstallationService {
    List<InstallationSummary> listInstallationsForUser(String userId);
}


