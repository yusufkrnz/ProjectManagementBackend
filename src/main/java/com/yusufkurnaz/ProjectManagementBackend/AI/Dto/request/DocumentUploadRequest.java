package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for document upload operations
 * Follows SRP - Single responsibility: Upload parameters validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    @Size(max = 10, message = "Maximum 10 user tags allowed")
    private List<@Size(max = 50, message = "User tag cannot exceed 50 characters") String> userTags;

    @Size(max = 10, message = "Maximum 10 domain tags allowed")
    private List<@Size(max = 50, message = "Domain tag cannot exceed 50 characters") String> domainTags;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Builder.Default
    private String languageCode = "tr";

    @Builder.Default
    private Boolean extractTextImmediately = true;

    @Builder.Default
    private Boolean generateEmbeddings = true;

    /**
     * Get clean user tags
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
     * Get clean domain tags
     */
    public List<String> getCleanDomainTags() {
        if (domainTags == null) return List.of();
        return domainTags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .distinct()
                .toList();
    }
}
