package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.Workspace;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.WorkspaceUser;
import com.yusufkurnaz.ProjectManagementBackend.Common.Service.HashService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Service.KeyManagementService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Exceptions.AuthenticationException;
import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.request.LoginRequest;
import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Controller.response.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    @Autowired
    private HashService hashService;
    
    @Autowired
    private KeyManagementService keyManagementService;
    
    @Autowired
    private JwtService jwtService;
    
    // TODO: Repository'ler eklenecek
    // @Autowired
    // private UserRepository userRepository;
    
    /**
     * Kullanıcı giriş işlemi
     * @param request Login request
     * @return Auth response
     */
    public AuthResponse authenticate(LoginRequest request) {
        try {
            // 1. Email hash'ini oluştur
            String emailHash = hashService.hashEmail(request.getEmail());
            
            // 2. User'ı email hash ile bul
            // TODO: Repository eklenecek
            // Optional<User> userOpt = userRepository.findByEmailHash(emailHash);
            // if (userOpt.isEmpty()) {
            //     throw new AuthenticationException("Invalid credentials");
            // }
            // User user = userOpt.get();
            
            // Şimdilik mock user oluşturalım
            User user = createMockUser(request.getEmail());
            
            // 3. Password'ü kontrol et
            boolean isValidPassword = hashService.verifyPassword(request.getPassword(), user.getPasswordHash());
            if (!isValidPassword) {
                throw new AuthenticationException("Invalid credentials");
            }
            
            // 4. Hesap kilitli mi kontrol et
            if (user.isAccountLocked()) {
                throw new AuthenticationException("Account is locked");
            }
            
            // 5. Email doğrulanmış mı kontrol et
            if (!user.isEmailVerified()) {
                throw new AuthenticationException("Email not verified");
            }
            
            // 6. Workspace kontrolü
            WorkspaceUser workspaceUser = getWorkspaceUser(user, request.getWorkspaceId());
            
            // 7. JWT token oluştur
            String accessToken = jwtService.generateAccessToken(user, workspaceUser.getWorkspace());
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // 8. Son giriş zamanını güncelle
            user.updateLastLogin();
            // TODO: userRepository.save(user);
            
            // 9. Response oluştur
            return buildAuthResponse(accessToken, refreshToken, user, workspaceUser);
            
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed", e);
        }
    }
    
    /**
     * Mock user oluşturur (şimdilik)
     */
    private User createMockUser(String email) {
        String passwordHash = hashService.hashPassword("123456"); // Test password
        
        return User.builder()
                
                .id(UUID.randomUUID())
                .emailHash(hashService.hashEmail(email))
                .emailEncrypted("encrypted_" + email)
                .passwordHash(passwordHash)
                .firstNameEncrypted("encrypted_Yusuf")
                .lastNameEncrypted("encrypted_Kurnaz")
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .build();
    }
    
    /**
     * Workspace user bilgisini alır
     */
    private WorkspaceUser getWorkspaceUser(User user, String workspaceId) {
        // TODO: WorkspaceUserRepository eklenecek
        // Mock workspace user oluşturalım
        
        Workspace workspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name("Test Workspace")
                .schemaName("workspace_test")
                .status(Workspace.WorkspaceStatus.ACTIVE)
                .build();
        
        return WorkspaceUser.builder()
                .id(UUID.randomUUID())
                .user(user)
                .workspace(workspace)
                .role(WorkspaceUser.UserRole.ADMIN)
                .status(WorkspaceUser.WorkspaceUserStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Auth response oluşturur
     */
    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user, WorkspaceUser workspaceUser) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId().toString())
                .email("yusuf@example.com") // Decrypt edilecek
                .firstName("Yusuf") // Decrypt edilecek
                .lastName("Kurnaz") // Decrypt edilecek
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().toString())
                .lastLoginAt(user.getLastLoginAt())
                .build();
        
        AuthResponse.WorkspaceInfo workspaceInfo = AuthResponse.WorkspaceInfo.builder()
                .id(workspaceUser.getWorkspace().getId().toString())
                .name(workspaceUser.getWorkspace().getName())
                .role(workspaceUser.getRole().toString())
                .permissions(List.of("READ_PROJECTS", "WRITE_TASKS")) // TODO: Permission sistemi
                .build();
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 hour
                .user(userInfo)
                .workspace(workspaceInfo)
                .build();
    }
}
