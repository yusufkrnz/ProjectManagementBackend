package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating canvas data (Excalidraw JSON)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasDataUpdateRequest {

    @NotBlank(message = "Canvas data is required")
    private String canvasData;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

    // Optional metadata for tracking changes
    private String changeDescription;
    @Builder.Default
    private Boolean autoSave = false;
}
