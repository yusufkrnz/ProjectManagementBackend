package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkspaceUser extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkspaceUserStatus status = WorkspaceUserStatus.ACTIVE;
    
    @Column(name = "invited_by")
    private UUID invitedBy;
    
    @Column(name = "invitation_token")
    private String invitationToken;
    
    @Column(name = "invitation_expires_at")
    private LocalDateTime invitationExpiresAt;
    
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions; // JSON permissions
    
    // User Role Enum
    public enum UserRole {
        ADMIN("Administrator", "Full access to workspace"),
        PROJECT_MANAGER("Project Manager", "Manage projects and tasks"),
        DEVELOPER("Developer", "Work on assigned tasks"),
        DESIGNER("Designer", "Design and UI/UX work"),
        QA("Quality Assurance", "Testing and quality control"),
        DEVOPS("DevOps Engineer", "Infrastructure and deployment"),
        FRONTEND_DEV("Frontend Developer", "Frontend development"),
        BACKEND_DEV("Backend Developer", "Backend development"),
        BACKEND_LEAD("Backend Lead", "Lead backend development"),
        PRODUCT_OWNER("Product Owner", "Product requirements and planning"),
        RESEARCHER("Researcher", "Market and user research"),
        VIEWER("Viewer", "Read-only access");
        
        private final String displayName;
        private final String description;
        
        UserRole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Workspace User Status Enum
    public enum WorkspaceUserStatus {
        ACTIVE, INACTIVE, PENDING_INVITATION, SUSPENDED
    }
    
    // Helper methods
    public boolean isActive() {
        return status == WorkspaceUserStatus.ACTIVE;
    }
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    public boolean isManager() {
        return role == UserRole.PROJECT_MANAGER || role == UserRole.PRODUCT_OWNER;
    }
    
    public boolean isDeveloper() {
        return role == UserRole.DEVELOPER || 
               role == UserRole.FRONTEND_DEV || 
               role == UserRole.BACKEND_DEV || 
               role == UserRole.BACKEND_LEAD;
    }
    
    public boolean hasValidInvitation() {
        return invitationToken != null && 
               invitationExpiresAt != null && 
               invitationExpiresAt.isAfter(LocalDateTime.now());
    }
    
    public void acceptInvitation() {
        status = WorkspaceUserStatus.ACTIVE;
        joinedAt = LocalDateTime.now();
        invitationToken = null;
        invitationExpiresAt = null;
    }
    
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
