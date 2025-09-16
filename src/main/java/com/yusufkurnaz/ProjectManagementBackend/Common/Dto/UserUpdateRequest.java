package com.yusufkurnaz.ProjectManagementBackend.Common.Dto;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    @Valid
    public void validate() {
        if (username == null) {
            throw new ValidationException("Username is required");
        }
        if (email == null) {
            throw new ValidationException("Email is required");
        }
    }

    public void updateUser(User user) {
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
    }

  
}