package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import com.yusufkurnaz.ProjectManagementBackend.Common.Repository.UserRepository;
import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.request.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public String login(LoginRequest request) {
        // Authentication manager ile kullanıcıyı doğrula
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // UserDetails'i al
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // JWT token oluştur
        return jwtService.generateToken(userDetails);
    }

    public String refreshToken(String token) {
        // Token'dan username'i çıkar
        String username = jwtService.extractUsername(token);
        
        // User'ı database'den bul
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Yeni token oluştur
        return jwtService.generateToken(user);
    }

    public String logout(String token) {
        // JWT stateless olduğu için logout'ta token'ı blacklist'e ekleyebiliriz
        // Şimdilik basit bir mesaj döndürüyoruz
        return "Successfully logged out";
    }

    public User me() {
        // SecurityContext'ten current user'ı al
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        
        throw new RuntimeException("No authenticated user found");
    }
}
