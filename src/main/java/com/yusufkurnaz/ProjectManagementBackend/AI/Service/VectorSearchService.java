package com.yusufkurnaz.ProjectManagementBackend.AI.Service;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;

import java.util.List;

/**
 * Service interface for vector-based similarity search
 * Follows SRP - Single responsibility: Vector similarity operations
 */
public interface VectorSearchService {

    /**
     * Find similar documents/chunks based on text query
     * Main discovery/research feature
     */
    List<DocumentChunk> findSimilarContent(
            String queryText, 
            List<String> domainTags, 
            Float minSimilarityScore,
            Integer limit
    );

    /**
     * Find similar documents by existing chunk
     */
    List<DocumentChunk> findSimilarToChunk(
            String chunkId, 
            List<String> domainTags,
            Integer limit
    );

    /**
     * Semantic search across all documents
     */
    List<DocumentChunk> semanticSearch(
            String searchQuery,
            List<String> includeTypes, // content types to include
            List<String> excludeTypes, // content types to exclude
            Integer limit
    );

    /**
     * Get embedding vector for text
     * Used internally by other services
     */
    float[] getTextEmbedding(String text);

    /**
     * Calculate similarity between two text embeddings
     */
    float calculateSimilarity(float[] embedding1, float[] embedding2);

    /**
     * Find related content suggestions
     */
    List<DocumentChunk> getRelatedContent(
            String currentContent,
            List<String> userDomainTags,
            Integer limit
    );

    /**
     * Get content recommendations based on user's previous documents
     */
    List<DocumentChunk> getPersonalizedRecommendations(
            String userId,
            Integer limit
    );
}
