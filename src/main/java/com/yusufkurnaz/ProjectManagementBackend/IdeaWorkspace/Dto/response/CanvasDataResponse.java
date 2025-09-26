package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Canvas Data (Excalidraw JSON)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasDataResponse {

    private UUID canvasId;
    private String canvasData;
    private Long version;
    private LocalDateTime lastUpdated;
    private UUID lastUpdatedBy;
    private String lastUpdatedByUsername;

    // Collaboration info
    private List<OnlineCollaboratorInfo> onlineCollaborators;
    private Boolean collaborationEnabled;

    // Canvas metadata
    private String canvasName;
    private Boolean canEdit;
    private Boolean canManage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OnlineCollaboratorInfo {
        private UUID userId;
        private String username;
        private String userColor;
        private Double cursorX;
        private Double cursorY;
        private String permission;
        private LocalDateTime lastActive;
    }
}
