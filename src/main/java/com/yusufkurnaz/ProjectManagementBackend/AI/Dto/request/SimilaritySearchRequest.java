package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for similarity search operations
 * Follows SRP - Single responsibility: Search parameters validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilaritySearchRequest {

    @NotBlank(message = "Search query is required")
    @Size(max = 1000, message = "Search query cannot exceed 1000 characters")
    private String query;

    @Size(max = 10, message = "Maximum 10 domain tags allowed")
    private List<@Size(max = 50, message = "Domain tag cannot exceed 50 characters") String> domainTags;

    @DecimalMin(value = "0.0", message = "Minimum similarity score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Minimum similarity score must be between 0.0 and 1.0")
    @Builder.Default
    private Float minSimilarityScore = 0.7f;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 50, message = "Limit cannot exceed 50")
    @Builder.Default
    private Integer limit = 10;

    @Size(max = 10, message = "Maximum 10 content types allowed")
    private List<String> includeContentTypes;

    @Size(max = 10, message = "Maximum 10 content types allowed")
    private List<String> excludeContentTypes;

    @Builder.Default
    private Boolean includePublicOnly = false;

    @Builder.Default
    private String languageCode = "tr";

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

    /**
     * Get clean include content types
     */
    public List<String> getCleanIncludeContentTypes() {
        if (includeContentTypes == null) return List.of();
        return includeContentTypes.stream()
                .filter(type -> type != null && !type.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * Get clean exclude content types
     */
    public List<String> getCleanExcludeContentTypes() {
        if (excludeContentTypes == null) return List.of();
        return excludeContentTypes.stream()
                .filter(type -> type != null && !type.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * Validate search parameters
     */
    public boolean isValid() {
        return query != null && !query.trim().isEmpty() && 
               limit > 0 && limit <= 50 &&
               minSimilarityScore >= 0.0f && minSimilarityScore <= 1.0f;
    }
}
