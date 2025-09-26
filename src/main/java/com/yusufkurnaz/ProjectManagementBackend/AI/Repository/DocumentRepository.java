package com.yusufkurnaz.ProjectManagementBackend.AI.Repository;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.FileType;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Document entity
 * Follows ISP - Interface segregation with specific document operations
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Find documents by uploader
     */
    Page<Document> findByUploadedByAndIsActiveTrueOrderByCreatedAtDesc(UUID uploadedBy, Pageable pageable);

    /**
     * Find documents by processing status
     */
    List<Document> findByProcessingStatusAndIsActiveTrue(ProcessingStatus processingStatus);

    /**
     * Find documents by file type
     */
    List<Document> findByFileTypeAndIsActiveTrueOrderByCreatedAtDesc(FileType fileType);

    /**
     * Find document by content hash (duplicate detection)
     */
    Optional<Document> findByContentHashAndIsActiveTrue(String contentHash);

    /**
     * Find documents by domain tags - ⭐ ETİKETLEME SİSTEMİ
     */
    @Query(value = """
        SELECT DISTINCT d.* FROM ai_documents d
        INNER JOIN document_domain_tags ddt ON d.id = ddt.document_id
        WHERE ddt.tag = ANY(CAST(:domainTags AS text[]))
        AND d.is_active = true
        ORDER BY d.created_at DESC
        """, nativeQuery = true)
    List<Document> findByDomainTags(@Param("domainTags") String[] domainTags);

    /**
     * Find documents by user tags
     */
    @Query(value = """
        SELECT DISTINCT d.* FROM ai_documents d
        INNER JOIN document_user_tags dut ON d.id = dut.document_id
        WHERE dut.tag = ANY(CAST(:userTags AS text[]))
        AND d.is_active = true
        ORDER BY d.created_at DESC
        """, nativeQuery = true)
    List<Document> findByUserTags(@Param("userTags") String[] userTags);

    /**
     * Find documents with successful processing and chunks
     */
    @Query("SELECT d FROM Document d WHERE d.processingStatus = 'COMPLETED' " +
           "AND d.totalChunks > 0 AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findProcessedDocumentsWithChunks();

    /**
     * Find documents for reprocessing (failed or incomplete)
     */
    @Query("SELECT d FROM Document d WHERE (d.processingStatus = 'FAILED' " +
           "OR (d.processingStatus = 'COMPLETED' AND d.totalChunks = 0)) " +
           "AND d.isActive = true ORDER BY d.createdAt ASC")
    List<Document> findDocumentsForReprocessing();

    /**
     * Find documents uploaded in date range
     */
    @Query("SELECT d FROM Document d WHERE d.createdAt BETWEEN :startDate AND :endDate " +
           "AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get document statistics by user
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_documents,
            COUNT(CASE WHEN processing_status = 'COMPLETED' THEN 1 END) as completed_documents,
            COUNT(CASE WHEN processing_status = 'FAILED' THEN 1 END) as failed_documents,
            SUM(file_size) as total_file_size,
            SUM(total_chunks) as total_chunks
        FROM ai_documents 
        WHERE uploaded_by = :userId AND is_active = true
        """, nativeQuery = true)
    Object getDocumentStatisticsByUser(@Param("userId") UUID userId);

    /**
     * Find popular domain tags
     */
    @Query(value = """
        SELECT ddt.tag, COUNT(*) as usage_count
        FROM document_domain_tags ddt
        INNER JOIN ai_documents d ON ddt.document_id = d.id
        WHERE d.is_active = true
        GROUP BY ddt.tag
        ORDER BY usage_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPopularDomainTags(@Param("limit") Integer limit);

    /**
     * Find documents by filename pattern
     */
    @Query("SELECT d FROM Document d WHERE LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :pattern, '%')) " +
           "AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByFilenamePattern(@Param("pattern") String pattern);

    /**
     * Count documents by processing status
     */
    @Query("SELECT d.processingStatus, COUNT(d) FROM Document d WHERE d.isActive = true " +
           "GROUP BY d.processingStatus")
    List<Object[]> countDocumentsByProcessingStatus();

    /**
     * Find large documents (for optimization)
     */
    @Query("SELECT d FROM Document d WHERE d.fileSize > :sizeThreshold " +
           "AND d.isActive = true ORDER BY d.fileSize DESC")
    List<Document> findLargeDocuments(@Param("sizeThreshold") Long sizeThreshold);

    /**
     * Find documents with low quality scores
     */
    @Query("SELECT d FROM Document d WHERE d.qualityScore < :qualityThreshold " +
           "AND d.qualityScore IS NOT NULL AND d.isActive = true " +
           "ORDER BY d.qualityScore ASC")
    List<Document> findLowQualityDocuments(@Param("qualityThreshold") Float qualityThreshold);

    /**
     * Search documents by text content (full-text search)
     */
    @Query(value = """
        SELECT d.* FROM ai_documents d 
        WHERE to_tsvector('turkish', COALESCE(d.extracted_text, '')) @@ plainto_tsquery('turkish', :searchText)
        AND d.is_active = true
        ORDER BY ts_rank(to_tsvector('turkish', COALESCE(d.extracted_text, '')), plainto_tsquery('turkish', :searchText)) DESC
        """, nativeQuery = true)
    List<Document> searchByTextContent(@Param("searchText") String searchText);
}
