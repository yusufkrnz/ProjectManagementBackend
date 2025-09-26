package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new canvas board
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasCreateRequest {

    @NotBlank(message = "Canvas name is required")
    @Size(min = 1, max = 255, message = "Canvas name must be between 1 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Workspace ID is required")
    private UUID workspaceId;

    // Canvas settings
    @Builder.Default
    private Integer canvasWidth = 1920;
    @Builder.Default
    private Integer canvasHeight = 1080;
    @Builder.Default
    private String backgroundColor = "#ffffff";
    @Builder.Default
    private Boolean gridEnabled = true;

    // Collaboration settings
    @Builder.Default
    private Boolean collaborationEnabled = true;
    @Builder.Default
    private Integer maxCollaborators = 10;

    // Initial canvas data (optional)
    private String canvasData;

    // Tags
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // Template settings
    @Builder.Default
    private Boolean isTemplate = false;
    @Builder.Default
    private Boolean isPublic = false;

    // Create from template
    private UUID templateId;
}
