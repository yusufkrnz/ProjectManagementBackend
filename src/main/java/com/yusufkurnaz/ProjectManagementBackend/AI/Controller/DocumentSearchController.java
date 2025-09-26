package com.yusufkurnaz.ProjectManagementBackend.AI.Controller;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.VectorSearchService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for RAG Vector Search operations
 * Provides semantic search and document discovery features
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Document Search", description = "RAG-based semantic search and document discovery")
@RequiredArgsConstructor
@Slf4j
public class DocumentSearchController {

    private final VectorSearchService vectorSearchService;

    @PostMapping("/semantic")
    @Operation(summary = "Semantic Search", 
               description = "Search documents using semantic similarity with user query")
    public ResponseEntity<ApiResponse<List<DocumentChunk>>> semanticSearch(
            @RequestParam String query,
            @RequestParam(required = false) List<String> domainTags,
            @RequestParam(defaultValue = "0.7") Float minSimilarity,
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Semantic search query: '{}' by user: {}", query, userId);

        List<DocumentChunk> results = vectorSearchService.findSimilarContent(
                query, domainTags, minSimilarity, limit);

        return ResponseEntity.ok(
                ApiResponse.success(results, "Semantic search completed successfully"));
    }

    @PostMapping("/similar-to-chunk")
    @Operation(summary = "Find Similar to Chunk", 
               description = "Find documents similar to a specific chunk")
    public ResponseEntity<ApiResponse<List<DocumentChunk>>> findSimilarToChunk(
            @RequestParam String chunkId,
            @RequestParam(required = false) List<String> domainTags,
            @RequestParam(defaultValue = "5") Integer limit,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Finding similar documents to chunk: {} by user: {}", chunkId, userId);

        List<DocumentChunk> results = vectorSearchService.findSimilarToChunk(
                chunkId, domainTags, limit);

        return ResponseEntity.ok(
                ApiResponse.success(results, "Similar chunks found successfully"));
    }

    @PostMapping("/discover")
    @Operation(summary = "Document Discovery", 
               description = "Discover relevant documents based on content and user domain tags")
    public ResponseEntity<ApiResponse<List<DocumentChunk>>> discoverDocuments(
            @RequestParam String searchQuery,
            @RequestParam(required = false) List<String> includeTypes,
            @RequestParam(required = false) List<String> excludeTypes,
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Document discovery query: '{}' by user: {}", searchQuery, userId);

        List<DocumentChunk> results = vectorSearchService.semanticSearch(
                searchQuery, includeTypes, excludeTypes, limit);

        return ResponseEntity.ok(
                ApiResponse.success(results, "Document discovery completed successfully"));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get Personalized Recommendations", 
               description = "Get personalized document recommendations based on user history")
    public ResponseEntity<ApiResponse<List<DocumentChunk>>> getRecommendations(
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Getting personalized recommendations for user: {}", userId);

        List<DocumentChunk> results = vectorSearchService.getPersonalizedRecommendations(
                userId.toString(), limit);

        return ResponseEntity.ok(
                ApiResponse.success(results, "Personalized recommendations retrieved successfully"));
    }

    @PostMapping("/related-content")
    @Operation(summary = "Get Related Content", 
               description = "Find content related to current user's work and domain tags")
    public ResponseEntity<ApiResponse<List<DocumentChunk>>> getRelatedContent(
            @RequestBody String currentContent,
            @RequestParam(required = false) List<String> userDomainTags,
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.info("Getting related content for user: {} with {} domain tags", userId, 
                userDomainTags != null ? userDomainTags.size() : 0);

        List<DocumentChunk> results = vectorSearchService.getRelatedContent(
                currentContent, userDomainTags, limit);

        return ResponseEntity.ok(
                ApiResponse.success(results, "Related content found successfully"));
    }

    @PostMapping("/embedding")
    @Operation(summary = "Get Text Embedding", 
               description = "Get vector embedding for given text (for testing purposes)")
    public ResponseEntity<ApiResponse<float[]>> getTextEmbedding(
            @RequestBody String text,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.debug("Getting embedding for text by user: {}", userId);

        float[] embedding = vectorSearchService.getTextEmbedding(text);

        return ResponseEntity.ok(
                ApiResponse.success(embedding, "Text embedding generated successfully"));
    }

    @PostMapping("/similarity")
    @Operation(summary = "Calculate Similarity", 
               description = "Calculate similarity between two text embeddings")
    public ResponseEntity<ApiResponse<Float>> calculateSimilarity(
            @RequestParam String text1,
            @RequestParam String text2,
            @Parameter(hidden = true) @RequestHeader("X-User-ID") UUID userId) {

        log.debug("Calculating similarity between texts by user: {}", userId);

        float[] embedding1 = vectorSearchService.getTextEmbedding(text1);
        float[] embedding2 = vectorSearchService.getTextEmbedding(text2);
        
        float similarity = vectorSearchService.calculateSimilarity(embedding1, embedding2);

        return ResponseEntity.ok(
                ApiResponse.success(similarity, "Similarity calculated successfully"));
    }
}
