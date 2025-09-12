package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Workspace extends BaseEntity {
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "schema_name", unique = true, nullable = false)
    private String schemaName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;
    
    @Column(name = "max_users")
    private Integer maxUsers = 100;
    
    @Column(name = "current_users")
    private Integer currentUsers = 0;
    
    @Column(name = "subscription_plan")
    private String subscriptionPlan = "FREE";
    
    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;
    
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON settings
    
    // Relationships
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkspaceUser> workspaceUsers = new HashSet<>();
    
    // Workspace Status Enum
    public enum WorkspaceStatus {
        ACTIVE, INACTIVE, SUSPENDED, TRIAL, EXPIRED
    }
    
    // Helper methods
    public boolean isActive() {
        return status == WorkspaceStatus.ACTIVE;
    }
    
    public boolean canAddUser() {
        return currentUsers == null || currentUsers < maxUsers;
    }
    
    public boolean isSubscriptionValid() {
        if (subscriptionExpiresAt == null) {
            return true; // Free plan
        }
        return subscriptionExpiresAt.isAfter(LocalDateTime.now());
    }
    
    public void incrementUserCount() {
        if (currentUsers == null) {
            currentUsers = 0;
        }
        currentUsers++;
    }
    
    public void decrementUserCount() {
        if (currentUsers == null) {
            currentUsers = 0;
        }
        if (currentUsers > 0) {
            currentUsers--;
        }
    }
    
    public String generateSchemaName() {
        if (schemaName == null || schemaName.isEmpty()) {
            schemaName = "workspace_" + this.getId().toString().replace("-", "_");
        }
        return schemaName;
    }
}
