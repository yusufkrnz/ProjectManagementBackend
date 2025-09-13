package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.request.LoginRequest;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;

/**
 * AuthService - Authentication ile ilgili işlemleri yapan servis.
 */
@Service
@RequiredArgsConstructor
@Tag(name = "Auth Service", description = "Authentication service endpoints")
public class AuthService {

    private final JwtService jwtService; // JWT ile ilgili işlemleri çağırmak için

    @Operation(summary = "Login", description = "Kullanıcı giriş işlemi")
    public String login(LoginRequest request) {
        // Burada DB kontrolü ve token üretme mantığı olacak
        User user = new User(); // örnek - gerçek User DB’den gelmeli
        return jwtService.generateToken(user);
    }

    @Operation(summary = "Refresh Token", description = "Var olan access token yenileme işlemi")
    public String refreshToken(String token) {
        return jwtService.generateRefreshToken(new User()); // örnek
    }

    @Operation(summary = "Logout", description = "Kullanıcı çıkış işlemi")
    public String logout(String token) {
        // JWT’de genelde logout olmaz, ama black-list mantığı eklenebilir
        return "Logged out: " + token;
    }

    @Operation(summary = "Validate Credentials", description = "Kullanıcı adı ve şifre kontrolü")
    public boolean validateCredentials(String username, String password) {
        // Şimdilik sabit true dönüyor, DB kontrolü eklenmeli
        return true;
    }

    @Operation(summary = "Get Current User", description = "Mevcut kullanıcı bilgisi")
    public User me() {
        // Şimdilik örnek user dönüyor
        User user = new User();
        user.setUsername("current-user");
        user.setEmail("user@example.com");
        user.setPassword("dummy");
        return user;
    }
}
