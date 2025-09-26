package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.response;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasBoard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Canvas Board
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID workspaceId;
    private String workspaceName;

    // Canvas settings
    private Integer canvasWidth;
    private Integer canvasHeight;
    private String backgroundColor;
    private Boolean gridEnabled;

    // Collaboration settings
    private Boolean collaborationEnabled;
    private Integer maxCollaborators;

    // Status and visibility
    private Boolean isPublic;
    private Boolean isTemplate;
    private Boolean isActive;

    // Statistics
    private Long viewCount;
    private Long forkCount;
    private Long version;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastAccessedAt;

    // User info
    private UUID createdBy;
    private String createdByUsername;
    private UUID updatedBy;
    private String updatedByUsername;

    // Tags
    private List<String> tags;

    // Collaboration info
    private Long collaboratorCount;
    private Long onlineCollaboratorCount;

    // Thumbnail
    private String thumbnailUrl;

    // User permissions
    private String userPermission; // VIEWER, EDITOR, ADMIN
    private Boolean canEdit;
    private Boolean canManage;

    /**
     * Convert CanvasBoard entity to response DTO
     */
    public static CanvasResponse fromEntity(CanvasBoard canvas) {
        return CanvasResponse.builder()
                .id(canvas.getId())
                .name(canvas.getName())
                .description(canvas.getDescription())
                .workspaceId(canvas.getWorkspaceId())
                .workspaceName(canvas.getWorkspace() != null ? canvas.getWorkspace().getName() : null)
                .canvasWidth(canvas.getCanvasWidth())
                .canvasHeight(canvas.getCanvasHeight())
                .backgroundColor(canvas.getBackgroundColor())
                .gridEnabled(canvas.getGridEnabled())
                .collaborationEnabled(canvas.getCollaborationEnabled())
                .maxCollaborators(canvas.getMaxCollaborators())
                .isPublic(canvas.getIsPublic())
                .isTemplate(canvas.getIsTemplate())
                .isActive(canvas.getIsActive())
                .viewCount(canvas.getViewCount())
                .forkCount(canvas.getForkCount())
                .version(canvas.getVersion())
                .createdAt(canvas.getCreatedAt())
                .updatedAt(canvas.getUpdatedAt())
                .lastAccessedAt(canvas.getLastAccessedAt())
                .createdBy(canvas.getCreatedBy())
                .updatedBy(canvas.getUpdatedBy())
                .tags(canvas.getTags())
                .thumbnailUrl(canvas.getThumbnailUrl())
                .build();
    }

    /**
     * Convert CanvasBoard entity to response DTO with additional info
     */
    public static CanvasResponse fromEntityWithDetails(CanvasBoard canvas, 
                                                     String createdByUsername, 
                                                     String updatedByUsername,
                                                     Long collaboratorCount,
                                                     Long onlineCollaboratorCount,
                                                     String userPermission,
                                                     Boolean canEdit,
                                                     Boolean canManage) {
        CanvasResponse response = fromEntity(canvas);
        response.setCreatedByUsername(createdByUsername);
        response.setUpdatedByUsername(updatedByUsername);
        response.setCollaboratorCount(collaboratorCount);
        response.setOnlineCollaboratorCount(onlineCollaboratorCount);
        response.setUserPermission(userPermission);
        response.setCanEdit(canEdit);
        response.setCanManage(canManage);
        return response;
    }
}
