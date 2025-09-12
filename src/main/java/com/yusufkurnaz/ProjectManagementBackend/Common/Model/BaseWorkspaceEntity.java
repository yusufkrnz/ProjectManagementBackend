package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@MappedSuperclass
@Data
public class BaseWorkspaceEntity extends BaseEntity {

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "workspace_encryption_key_id", nullable = false)
    private String workspaceEncryptionKeyId;

    @PrePersist
    @PreUpdate
    protected void validateWorkspace() {
        if(workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID is required");
        }
    }
}
