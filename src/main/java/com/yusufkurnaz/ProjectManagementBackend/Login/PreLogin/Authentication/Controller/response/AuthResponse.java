package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    
    private UserInfo user;
    private WorkspaceInfo workspace;
    
    @Data
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private String status;
        private LocalDateTime lastLoginAt;
    }
    
    @Data
    @Builder
    public static class WorkspaceInfo {
        private String id;
        private String name;
        private String role;
        private List<String> permissions;
    }
}
