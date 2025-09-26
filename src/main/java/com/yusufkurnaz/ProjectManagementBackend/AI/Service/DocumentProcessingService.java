package com.yusufkurnaz.ProjectManagementBackend.AI.Service;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for document processing operations
 * Follows ISP - Interface segregation principle
 */
public interface DocumentProcessingService {

    /**
     * Process uploaded PDF file
     * Main entry point for document processing
     */
    Document processDocument(MultipartFile file, UUID userId, List<String> userTags);

    /**
     * Reprocess existing document (for failed or incomplete processing)
     */
    Document reprocessDocument(UUID documentId);

    /**
     * Extract text from document file
     */
    String extractTextFromFile(MultipartFile file);

    /**
     * Chunk document text into smaller pieces
     */
    List<DocumentChunk> chunkDocument(Document document, String extractedText);

    /**
     * Generate embeddings for document chunks
     * Background/async operation
     */
    void generateEmbeddingsForDocument(UUID documentId);

    /**
     * Add domain tags to document (AI-powered categorization)
     */
    Document addDomainTags(UUID documentId, List<String> domainTags);

    /**
     * Add user tags to document
     */
    Document addUserTags(UUID documentId, UUID userId, List<String> userTags);

    /**
     * Get document processing status
     */
    Document getDocumentStatus(UUID documentId);

    /**
     * Get user's documents with filtering
     */
    List<Document> getUserDocuments(UUID userId, String fileType, String processingStatus, int page, int size);

    /**
     * Delete document and all related data
     */
    void deleteDocument(UUID documentId, UUID userId);

    /**
     * Get document statistics for user
     */
    Object getDocumentStatistics(UUID userId);

    /**
     * Search documents by content
     */
    List<Document> searchDocuments(String searchText, List<String> domainTags, UUID userId);

    /**
     * Get popular domain tags for suggestions
     */
    List<String> getPopularDomainTags(int limit);

    /**
     * Validate file for processing
     */
    void validateFile(MultipartFile file);
}
