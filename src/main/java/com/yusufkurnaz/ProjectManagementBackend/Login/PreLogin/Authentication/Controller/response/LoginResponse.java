package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType="Bearer";
    private Long expiresIn;
}