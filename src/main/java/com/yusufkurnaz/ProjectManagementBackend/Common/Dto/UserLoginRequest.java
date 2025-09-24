package com.yusufkurnaz.ProjectManagementBackend.Common.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {
    private String username;
    private String password;
}