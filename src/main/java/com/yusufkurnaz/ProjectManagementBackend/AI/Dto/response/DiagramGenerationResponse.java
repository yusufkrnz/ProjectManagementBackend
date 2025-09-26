package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for diagram generation operations
 * Follows SRP - Single responsibility: Response data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagramGenerationResponse {

    private String diagramId;
    
    private String documentId;
    
    private DiagramType diagramType;
    
    private String title;
    
    private String description;
    
    private String svgContent;
    
    private String plantUmlCode;
    
    private List<String> tags;
    
    private Boolean isPublic;
    
    private Integer userRating;
    
    private String userFeedback;
    
    private Long viewCount;
    
    private Long downloadCount;
    
    private LocalDateTime createdAt;
    
    private Long generationTimeMs;
    
    private String llmModelUsed;
    
    private Float qualityScore;
    
    // Export URLs
    private String svgDownloadUrl;
    private String pngDownloadUrl;
    private String plantUmlDownloadUrl;
    
    // Related content
    private List<SimilarDiagramResponse> similarDiagrams;
    
    /**
     * Create response from entity (for service layer)
     */
    public static DiagramGenerationResponse fromEntity(Object diagram, String baseUrl) {
        // This will be implemented when we have the actual entity
        return DiagramGenerationResponse.builder()
                .build();
    }
    
    /**
     * Create minimal response (for listing operations)
     */
    public static DiagramGenerationResponse minimal(String diagramId, String title, DiagramType type, LocalDateTime createdAt) {
        return DiagramGenerationResponse.builder()
                .diagramId(diagramId)
                .title(title)
                .diagramType(type)
                .createdAt(createdAt)
                .build();
    }
}
