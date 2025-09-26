package com.yusufkurnaz.ProjectManagementBackend.AI.Controller;

import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request.SimilaritySearchRequest;
import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response.SimilaritySearchResponse;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.VectorSearchService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for vector similarity search operations
 * Follows SRP - Single responsibility: HTTP request handling for search
 */
@RestController
@RequestMapping("/api/v1/ai/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Search Controller", description = "Vector-based similarity search operations")
public class SearchController {

    private final VectorSearchService vectorSearchService;

    @PostMapping("/similarity")
    @Operation(summary = "Similarity search", 
               description = "Find similar content using vector similarity search")
    public ResponseEntity<ApiResponse<SimilaritySearchResponse>> similaritySearch(
            @Valid @RequestBody SimilaritySearchRequest request,
            Authentication authentication) {
        
        log.info("Performing similarity search for query: '{}' by user: {}", 
                request.getQuery(), authentication.getName());
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<DocumentChunk> results = vectorSearchService.findSimilarContent(
                    request.getQuery(),
                    request.getCleanDomainTags(),
                    request.getMinSimilarityScore(),
                    request.getLimit()
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            SimilaritySearchResponse response = buildSearchResponse(
                    request.getQuery(), results, searchTime
            );
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                    String.format("Found %d similar results", results.size())));
            
        } catch (Exception e) {
            log.error("Error performing similarity search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }

    @GetMapping("/semantic")
    @Operation(summary = "Semantic search", 
               description = "Perform semantic search across all documents")
    public ResponseEntity<ApiResponse<SimilaritySearchResponse>> semanticSearch(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Content types to include") @RequestParam(required = false) List<String> includeTypes,
            @Parameter(description = "Content types to exclude") @RequestParam(required = false) List<String> excludeTypes,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("Performing semantic search for query: '{}'", query);
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<DocumentChunk> results = vectorSearchService.semanticSearch(
                    query, includeTypes, excludeTypes, limit
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            SimilaritySearchResponse response = buildSearchResponse(query, results, searchTime);
            
            return ResponseEntity.ok(ApiResponse.success(response, 
                    String.format("Found %d semantic results", results.size())));
            
        } catch (Exception e) {
            log.error("Error performing semantic search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Semantic search failed: " + e.getMessage()));
        }
    }

    @GetMapping("/related")
    @Operation(summary = "Get related content", 
               description = "Find content related to provided text")
    public ResponseEntity<ApiResponse<SimilaritySearchResponse>> getRelatedContent(
            @Parameter(description = "Content to find related items for") @RequestParam String content,
            @Parameter(description = "Domain tags for filtering") @RequestParam(required = false) List<String> domainTags,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "5") Integer limit) {
        
        log.info("Finding related content for text of length: {}", content.length());
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<DocumentChunk> results = vectorSearchService.getRelatedContent(
                    content, domainTags, limit
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            SimilaritySearchResponse response = buildSearchResponse(
                    "Related to provided content", results, searchTime
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("Error finding related content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Related content search failed: " + e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get personalized recommendations", 
               description = "Get content recommendations based on user's previous documents")
    public ResponseEntity<ApiResponse<SimilaritySearchResponse>> getPersonalizedRecommendations(
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication) {
        
        log.info("Getting personalized recommendations for user: {}", authentication.getName());
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<DocumentChunk> results = vectorSearchService.getPersonalizedRecommendations(
                    authentication.getName(), limit
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            SimilaritySearchResponse response = buildSearchResponse(
                    "Personalized recommendations", results, searchTime
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("Error getting personalized recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Recommendations failed: " + e.getMessage()));
        }
    }

    @GetMapping("/chunk/{chunkId}/similar")
    @Operation(summary = "Find similar to chunk", 
               description = "Find content similar to a specific document chunk")
    public ResponseEntity<ApiResponse<SimilaritySearchResponse>> findSimilarToChunk(
            @Parameter(description = "Chunk ID") @PathVariable String chunkId,
            @Parameter(description = "Domain tags for filtering") @RequestParam(required = false) List<String> domainTags,
            @Parameter(description = "Maximum results") @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("Finding content similar to chunk: {}", chunkId);
        
        try {
            long startTime = System.currentTimeMillis();
            
            List<DocumentChunk> results = vectorSearchService.findSimilarToChunk(
                    chunkId, domainTags, limit
            );
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            SimilaritySearchResponse response = buildSearchResponse(
                    "Similar to chunk " + chunkId, results, searchTime
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("Error finding similar content to chunk", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Similar chunk search failed: " + e.getMessage()));
        }
    }

    /**
     * Build search response from results
     */
    private SimilaritySearchResponse buildSearchResponse(String query, List<DocumentChunk> chunks, Long searchTimeMs) {
        if (chunks.isEmpty()) {
            return SimilaritySearchResponse.empty(query, searchTimeMs);
        }
        
        List<SimilaritySearchResponse.SimilarContentResult> results = chunks.stream()
                .map(this::mapChunkToResult)
                .toList();
        
        SimilaritySearchResponse response = SimilaritySearchResponse.builder()
                .query(query)
                .results(results)
                .searchTimeMs(searchTimeMs)
                .build();
        
        response.calculateStatistics();
        
        return response;
    }

    /**
     * Map DocumentChunk to SimilarContentResult
     */
    private SimilaritySearchResponse.SimilarContentResult mapChunkToResult(DocumentChunk chunk) {
        return SimilaritySearchResponse.SimilarContentResult.builder()
                .chunkId(chunk.getId().toString())
                .documentId(chunk.getDocument().getId().toString())
                .documentTitle(chunk.getDocument().getOriginalFilename())
                .chunkText(chunk.getChunkText())
                .chunkSummary(chunk.getChunkSummary())
                .similarityScore(chunk.getConfidenceScore()) // This would be calculated in service
                .pageNumber(chunk.getPageNumber())
                .sectionTitle(chunk.getSectionTitle())
                .contentType(chunk.getContentType())
                .domainTags(chunk.getDocument().getDomainTags())
                .userTags(chunk.getDocument().getUserTags())
                .createdAt(chunk.getCreatedAt())
                .uploadedBy(chunk.getDocument().getUploadedBy().toString())
                .build();
    }
}
