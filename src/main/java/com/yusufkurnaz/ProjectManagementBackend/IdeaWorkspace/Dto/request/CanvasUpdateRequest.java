package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating canvas board metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasUpdateRequest {

    @Size(min = 1, max = 255, message = "Canvas name must be between 1 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    // Canvas settings
    private Integer canvasWidth;
    private Integer canvasHeight;
    private String backgroundColor;
    private Boolean gridEnabled;

    // Collaboration settings
    private Boolean collaborationEnabled;
    private Integer maxCollaborators;

    // Visibility settings
    private Boolean isPublic;
    private Boolean isTemplate;

    // Tags management
    private List<String> tags;
    private List<String> addTags;
    private List<String> removeTags;
}
