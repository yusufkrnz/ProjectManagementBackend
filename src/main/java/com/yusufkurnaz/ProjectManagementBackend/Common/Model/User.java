package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseEntity {
    
    // Email - Hash edilmiş (search için)
    @Column(name = "email_hash", unique = true, nullable = false)
    private String emailHash;
    
    // Email - Encrypted (decrypt edilebilir)
    @Column(name = "email_encrypted", nullable = false)
    private String emailEncrypted;
    
    // Password - BCrypt hash (one-way)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    // Kişisel veriler - AES encrypted
    @Column(name = "first_name_encrypted")
    private String firstNameEncrypted;
    
    @Column(name = "last_name_encrypted")
    private String lastNameEncrypted;
    
    @Column(name = "phone_number_encrypted")
    private String phoneNumberEncrypted;
    
    // Avatar URL - Plain text (public data)
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    // Status - Plain text (enum)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.ACTIVE;
    
    // Security fields
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "email_verification_token")
    private String emailVerificationToken;
    
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    
    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;
    
    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkspaceUser> workspaceUsers = new HashSet<>();
    
    // User Status Enum
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, LOCKED
    }
    
    // Helper methods
    public boolean isAccountLocked() {
        return status == UserStatus.LOCKED || 
               (failedLoginAttempts != null && failedLoginAttempts >= 5);
    }
    
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }
    
    public boolean hasValidPasswordResetToken() {
        return passwordResetToken != null && 
               passwordResetExpiresAt != null && 
               passwordResetExpiresAt.isAfter(LocalDateTime.now());
    }
    
    public void incrementFailedLoginAttempts() {
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        failedLoginAttempts++;
        
        if (failedLoginAttempts >= 5) {
            status = UserStatus.LOCKED;
        }
    }
    
    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        if (status == UserStatus.LOCKED) {
            status = UserStatus.ACTIVE;
        }
    }
    
    public void updateLastLogin() {
        lastLoginAt = LocalDateTime.now();
        resetFailedLoginAttempts();
    }
    
    public void updatePasswordChange() {
        passwordChangedAt = LocalDateTime.now();
        passwordResetToken = null;
        passwordResetExpiresAt = null;
    }
}
