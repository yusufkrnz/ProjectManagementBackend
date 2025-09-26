package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for similarity search operations
 * Follows SRP - Single responsibility: Search results data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimilaritySearchResponse {

    private String query;
    
    private Integer totalResults;
    
    private Float maxSimilarityScore;
    
    private Float minSimilarityScore;
    
    private Long searchTimeMs;
    
    private List<SimilarContentResult> results;
    
    private List<String> suggestedTags;
    
    private List<String> relatedQueries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SimilarContentResult {
        
        private String chunkId;
        
        private String documentId;
        
        private String documentTitle;
        
        private String chunkText;
        
        private String chunkSummary;
        
        private Float similarityScore;
        
        private Integer pageNumber;
        
        private String sectionTitle;
        
        private String contentType;
        
        private List<String> domainTags;
        
        private List<String> userTags;
        
        private LocalDateTime createdAt;
        
        private String uploadedBy;
        
        // Related diagrams generated from this document
        private List<SimilarDiagramResponse> relatedDiagrams;
    }
    
    /**
     * Create empty response for no results
     */
    public static SimilaritySearchResponse empty(String query, Long searchTimeMs) {
        return SimilaritySearchResponse.builder()
                .query(query)
                .totalResults(0)
                .searchTimeMs(searchTimeMs)
                .results(List.of())
                .build();
    }
    
    /**
     * Calculate search statistics
     */
    public void calculateStatistics() {
        if (results != null && !results.isEmpty()) {
            this.totalResults = results.size();
            this.maxSimilarityScore = results.stream()
                    .map(SimilarContentResult::getSimilarityScore)
                    .max(Float::compareTo)
                    .orElse(0.0f);
            this.minSimilarityScore = results.stream()
                    .map(SimilarContentResult::getSimilarityScore)
                    .min(Float::compareTo)
                    .orElse(0.0f);
        }
    }
}
