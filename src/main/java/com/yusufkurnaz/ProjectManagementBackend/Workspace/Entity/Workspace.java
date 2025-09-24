package com.yusufkurnaz.ProjectManagementBackend.Workspace.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "workspaces")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Workspace extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    public enum WorkspaceStatus {
        ACTIVE, INACTIVE, ARCHIVED
    }
}
