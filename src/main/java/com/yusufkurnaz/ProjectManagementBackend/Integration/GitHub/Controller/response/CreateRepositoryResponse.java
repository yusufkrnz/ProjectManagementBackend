package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateRepositoryResponse {
    private String htmlUrl;
    private String cloneUrl;
}


