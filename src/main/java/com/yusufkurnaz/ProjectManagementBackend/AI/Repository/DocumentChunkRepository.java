package com.yusufkurnaz.ProjectManagementBackend.AI.Repository;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DocumentChunk with vector similarity search capabilities
 * Follows ISP - Interface segregation with specific vector operations
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    /**
     * Find chunks by document ID
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);

    /**
     * Find chunks by document ID and page number
     */
    List<DocumentChunk> findByDocumentIdAndPageNumberOrderByChunkIndex(UUID documentId, Integer pageNumber);

    /**
     * Vector similarity search using pgvector
     * ⭐ EN ÖNEMLİ SORGU - Benzer dokümanları bul
     */
    @Query(value = """
        SELECT dc.* FROM document_chunks dc
        INNER JOIN ai_documents d ON dc.document_id = d.id
        WHERE dc.embedding IS NOT NULL 
        AND d.is_active = true
        AND (:domainTags IS NULL OR EXISTS (
            SELECT 1 FROM document_domain_tags ddt 
            WHERE ddt.document_id = d.id 
            AND ddt.tag = ANY(CAST(:domainTags AS text[]))
        ))
        ORDER BY dc.embedding <-> CAST(:queryEmbedding AS vector) 
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("domainTags") String[] domainTags,
            @Param("limit") Integer limit
    );

    /**
     * Vector similarity search with minimum similarity threshold
     */
    @Query(value = """
        SELECT dc.*, 
               (1 - (dc.embedding <-> CAST(:queryEmbedding AS vector))) as similarity_score
        FROM document_chunks dc
        INNER JOIN ai_documents d ON dc.document_id = d.id
        WHERE dc.embedding IS NOT NULL 
        AND d.is_active = true
        AND (1 - (dc.embedding <-> CAST(:queryEmbedding AS vector))) >= :minSimilarity
        AND (:domainTags IS NULL OR EXISTS (
            SELECT 1 FROM document_domain_tags ddt 
            WHERE ddt.document_id = d.id 
            AND ddt.tag = ANY(CAST(:domainTags AS text[]))
        ))
        ORDER BY similarity_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunksWithScore(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("minSimilarity") Float minSimilarity,
            @Param("domainTags") String[] domainTags,
            @Param("limit") Integer limit
    );

    /**
     * Find chunks by content type (for specific domain searches)
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.contentType = :contentType " +
           "AND dc.document.isActive = true ORDER BY dc.confidenceScore DESC")
    List<DocumentChunk> findByContentType(@Param("contentType") String contentType);

    /**
     * Find chunks with high confidence scores for quality filtering
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.confidenceScore >= :minConfidence " +
           "AND dc.embedding IS NOT NULL AND dc.document.isActive = true " +
           "ORDER BY dc.confidenceScore DESC")
    List<DocumentChunk> findHighQualityChunks(@Param("minConfidence") Float minConfidence);

    /**
     * Count chunks by document
     */
    @Query("SELECT COUNT(dc) FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    Long countByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Find chunks without embeddings (for reprocessing)
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.embedding IS NULL " +
           "AND dc.document.isActive = true ORDER BY dc.createdAt ASC")
    List<DocumentChunk> findChunksWithoutEmbeddings();

    /**
     * Find chunks by multiple domain tags (OR condition)
     */
    @Query(value = """
        SELECT DISTINCT dc.* FROM document_chunks dc
        INNER JOIN ai_documents d ON dc.document_id = d.id
        INNER JOIN document_domain_tags ddt ON d.id = ddt.document_id
        WHERE ddt.tag = ANY(CAST(:domainTags AS text[]))
        AND d.is_active = true
        AND dc.embedding IS NOT NULL
        ORDER BY dc.confidence_score DESC
        """, nativeQuery = true)
    List<DocumentChunk> findByDomainTags(@Param("domainTags") String[] domainTags);

    /**
     * Get chunk statistics for analytics
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_chunks,
            COUNT(CASE WHEN embedding IS NOT NULL THEN 1 END) as chunks_with_embeddings,
            AVG(confidence_score) as avg_confidence,
            AVG(token_count) as avg_token_count
        FROM document_chunks dc
        INNER JOIN ai_documents d ON dc.document_id = d.id
        WHERE d.is_active = true
        """, nativeQuery = true)
    Object getChunkStatistics();

    /**
     * Delete chunks by document ID (cascade cleanup)
     */
    void deleteByDocumentId(UUID documentId);
}
