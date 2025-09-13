package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service;

import org.springframework.stereotype.Service;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import io.swagger.v3.oas.annotations.tags.Tag;

@Service
@Tag(name = "Jwt Service", description = "Jwt service")
public class JwtService {

    public String generateToken(User user) {
        // Burada JWT üretim kodu olmalı
        return "fake-jwt-token-for-" + (user != null ? user.getUsername() : "unknown");
    }

    public String generateRefreshToken(User user) {
        return "fake-refresh-token-for-" + (user != null ? user.getUsername() : "unknown");
    }

    public boolean validateToken(String token) {
        // Normalde JWT library ile doğrulama yapılır
        return token != null && token.startsWith("fake");
    }

    public User getUserFromToken(String token) {
        // Token'dan user bilgisi çıkarılmalı
        User user = new User();
        user.setUsername("extracted-from-token");
        user.setEmail("extracted@example.com");
        user.setPassword("dummy");
        return user;
    }

    public boolean isTokenExpired(String token) {
        // Token expiration kontrolü yapılmalı
        return false;
    }
}
