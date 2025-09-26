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
 * Response DTO for similar diagram information
 * Follows SRP - Single responsibility: Similar diagram data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimilarDiagramResponse {

    private String diagramId;
    
    private String title;
    
    private DiagramType diagramType;
    
    private String description;
    
    private List<String> tags;
    
    private Integer userRating;
    
    private Long viewCount;
    
    private Long downloadCount;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private Boolean isPublic;
    
    private Float similarityScore;
    
    private String thumbnailUrl;
    
    private String previewUrl;
    
    /**
     * Create minimal response for listing
     */
    public static SimilarDiagramResponse minimal(String diagramId, String title, DiagramType type, Long viewCount) {
        return SimilarDiagramResponse.builder()
                .diagramId(diagramId)
                .title(title)
                .diagramType(type)
                .viewCount(viewCount)
                .build();
    }
}
