package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for diagram generation
 * Follows SRP - Single responsibility: Request data validation and transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramGenerationRequest {

    @NotNull(message = "Diagram type is required")
    private DiagramType diagramType;

    @Size(max = 2000, message = "Custom prompt cannot exceed 2000 characters")
    private String customPrompt;

    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<@Size(max = 50, message = "Tag cannot exceed 50 characters") String> tags;

    @Size(max = 10, message = "Maximum 10 user tags allowed")
    private List<@Size(max = 50, message = "User tag cannot exceed 50 characters") String> userTags;

    @Builder.Default
    private Boolean isPublic = false;

    // For regeneration requests
    private String existingDiagramId;

    // For document-based generation
    private String documentId;

    /**
     * Validate request for PDF upload scenario
     */
    public boolean isValidForPDFUpload() {
        return diagramType != null && documentId == null && existingDiagramId == null;
    }

    /**
     * Validate request for document-based generation
     */
    public boolean isValidForDocumentGeneration() {
        return diagramType != null && documentId != null && existingDiagramId == null;
    }

    /**
     * Validate request for regeneration
     */
    public boolean isValidForRegeneration() {
        return diagramType != null && existingDiagramId != null;
    }

    /**
     * Get clean user tags (trimmed and lowercase)
     */
    public List<String> getCleanUserTags() {
        if (userTags == null) return List.of();
        return userTags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .distinct()
                .toList();
    }

    /**
     * Get clean tags (trimmed and lowercase)
     */
    public List<String> getCleanTags() {
        if (tags == null) return List.of();
        return tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .distinct()
                .toList();
    }
}
