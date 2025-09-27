package com.yusufkurnaz.ProjectManagementBackend.AI.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentChunkRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.VectorSearchService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of VectorSearchService for semantic similarity search
 * Uses pgvector for efficient vector similarity queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VectorSearchServiceImpl implements VectorSearchService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    @Override
    public List<DocumentChunk> findSimilarContent(
            String queryText, 
            List<String> domainTags, 
            Float minSimilarityScore,
            Integer limit) {
        
        log.info("Searching for similar content with query: '{}'", queryText);
        
        try {
            // Generate embedding for the search query
            float[] queryEmbedding = embeddingService.embedText(queryText);
            String embeddingString = floatArrayToString(queryEmbedding);
            
            // Convert domain tags to array
            String[] domainTagsArray = domainTags != null ? 
                domainTags.toArray(new String[0]) : null;
            
            // Use similarity threshold search if specified
            if (minSimilarityScore != null && minSimilarityScore > 0) {
                List<Object[]> results = documentChunkRepository.findSimilarChunksWithScore(
                        embeddingString, 
                        minSimilarityScore, 
                        domainTagsArray, 
                        limit != null ? limit : 10
                );
                
                // Convert Object[] results to DocumentChunk entities
                List<DocumentChunk> chunks = new ArrayList<>();
                for (Object[] result : results) {
                    if (result[0] instanceof DocumentChunk) {
                        chunks.add((DocumentChunk) result[0]);
                    }
                }
                return chunks;
            } else {
                // Use basic similarity search
                return documentChunkRepository.findSimilarChunks(
                        embeddingString, 
                        domainTagsArray, 
                        limit != null ? limit : 10
                );
            }
            
        } catch (Exception e) {
            log.error("Error during vector search: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<DocumentChunk> findSimilarToChunk(
            String chunkId, 
            List<String> domainTags,
            Integer limit) {
        
        try {
            UUID chunkUuid = UUID.fromString(chunkId);
            DocumentChunk sourceChunk = documentChunkRepository.findById(chunkUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + chunkId));
            
            if (sourceChunk.getEmbedding() == null) {
                log.warn("Source chunk {} has no embedding", chunkId);
                return new ArrayList<>();
            }
            
            String[] domainTagsArray = domainTags != null ? 
                domainTags.toArray(new String[0]) : null;
            
            return documentChunkRepository.findSimilarChunks(
                    sourceChunk.getEmbedding(), 
                    domainTagsArray, 
                    limit != null ? limit : 10
            );
            
        } catch (Exception e) {
            log.error("Error finding similar chunks to {}: {}", chunkId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<DocumentChunk> semanticSearch(
            String searchQuery,
            List<String> includeTypes,
            List<String> excludeTypes,
            Integer limit) {
        
        try {
            float[] queryEmbedding = embeddingService.embedText(searchQuery);
            String embeddingString = floatArrayToString(queryEmbedding);
            
            // Basic search without type filtering for now
            // Implement content type filtering
            return documentChunkRepository.findSimilarChunks(
                    embeddingString, 
                    null, // No domain tag filtering
                    limit != null ? limit : 20
            );
            
        } catch (Exception e) {
            log.error("Error during semantic search: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public float[] getTextEmbedding(String text) {
        try {
            return embeddingService.embedText(text);
        } catch (Exception e) {
            log.error("Error generating embedding for text: {}", e.getMessage());
            return new float[0];
        }
    }

    @Override
    public float calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null || 
            embedding1.length == 0 || embedding2.length == 0 ||
            embedding1.length != embedding2.length) {
            return 0.0f;
        }
        
        // Cosine similarity calculation
        float dotProduct = 0.0f;
        float magnitude1 = 0.0f;
        float magnitude2 = 0.0f;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            magnitude1 += embedding1[i] * embedding1[i];
            magnitude2 += embedding2[i] * embedding2[i];
        }
        
        magnitude1 = (float) Math.sqrt(magnitude1);
        magnitude2 = (float) Math.sqrt(magnitude2);
        
        if (magnitude1 == 0.0f || magnitude2 == 0.0f) {
            return 0.0f;
        }
        
        return dotProduct / (magnitude1 * magnitude2);
    }

    @Override
    public List<DocumentChunk> getRelatedContent(
            String currentContent,
            List<String> userDomainTags,
            Integer limit) {
        
        return findSimilarContent(currentContent, userDomainTags, 0.3f, limit);
    }

    @Override
    public List<DocumentChunk> getPersonalizedRecommendations(
            String userId,
            Integer limit) {
        
        //Implement personalized recommendations based on user history
        // For now, return high-quality chunks
        return documentChunkRepository.findHighQualityChunks(0.8f)
                .stream()
                .limit(limit != null ? limit : 10)
                .toList();
    }

    /**
     * Convert float array to comma-separated string for database storage
     */
    private String floatArrayToString(float[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}