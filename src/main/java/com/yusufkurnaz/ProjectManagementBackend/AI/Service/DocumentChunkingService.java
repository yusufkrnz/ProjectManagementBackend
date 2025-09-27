package com.yusufkurnaz.ProjectManagementBackend.AI.Service;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for document chunking operations
 * Handles splitting documents into manageable chunks for vector processing
 */
public interface DocumentChunkingService {

    /**
     * Chunk a document into smaller text segments
     */
    List<DocumentChunk> chunkDocument(UUID documentId);

    /**
     * Chunk text directly with custom parameters
     */
    List<DocumentChunk> chunkText(String text, int maxChunkSize, int overlapSize);

    /**
     * Re-chunk an existing document with new parameters
     */
    List<DocumentChunk> rechunkDocument(UUID documentId, int newChunkSize, int newOverlapSize);

    /**
     * Get optimal chunk size for a document based on content type
     */
    int getOptimalChunkSize(Document document);

    /**
     * Get chunks for a specific document
     */
    List<DocumentChunk> getDocumentChunks(UUID documentId);

    /**
     * Get chunks for a specific page
     */
    List<DocumentChunk> getChunksByPage(UUID documentId, Integer pageNumber);

    /**
     * Delete all chunks for a document
     */
    void deleteDocumentChunks(UUID documentId);

    /**
     * Update chunk embeddings
     */
    void updateChunkEmbeddings(UUID documentId);

    /**
     * Get chunk statistics
     */
    ChunkStatistics getChunkStatistics(UUID documentId);

    /**
     * Validate chunk quality
     */
    boolean validateChunkQuality(DocumentChunk chunk);

    /**
     * Merge small chunks if needed
     */
    List<DocumentChunk> optimizeChunks(List<DocumentChunk> chunks);

    /**
     * Extract section titles from chunks
     */
    void extractSectionTitles(List<DocumentChunk> chunks);

    /**
     * Statistics for document chunking
     */
    record ChunkStatistics(
            int totalChunks,
            int averageChunkSize,
            int minChunkSize,
            int maxChunkSize,
            double averageOverlap,
            int emptyChunks,
            int qualityScore
    ) {}
}

