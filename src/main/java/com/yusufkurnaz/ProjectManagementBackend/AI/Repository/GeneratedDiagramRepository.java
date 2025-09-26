package com.yusufkurnaz.ProjectManagementBackend.AI.Repository;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.GeneratedDiagram;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for GeneratedDiagram entity
 * Follows ISP - Interface segregation with specific diagram operations
 */
@Repository
public interface GeneratedDiagramRepository extends JpaRepository<GeneratedDiagram, UUID> {

    /**
     * Find diagrams by user (owner)
     */
    Page<GeneratedDiagram> findByGeneratedByAndIsActiveTrueOrderByCreatedAtDesc(UUID generatedBy, Pageable pageable);

    /**
     * Find diagrams by source document
     */
    List<GeneratedDiagram> findBySourceDocumentIdAndIsActiveTrueOrderByCreatedAtDesc(UUID sourceDocumentId);

    /**
     * Find diagrams by type
     */
    List<GeneratedDiagram> findByDiagramTypeAndIsActiveTrueOrderByCreatedAtDesc(DiagramType diagramType);

    /**
     * Find public diagrams by type
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.diagramType = :diagramType " +
           "AND gd.isPublic = true AND gd.isActive = true ORDER BY gd.viewCount DESC, gd.createdAt DESC")
    List<GeneratedDiagram> findPublicDiagramsByType(@Param("diagramType") DiagramType diagramType, Pageable pageable);

    /**
     * Find popular public diagrams (most viewed)
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.isPublic = true AND gd.isActive = true " +
           "ORDER BY gd.viewCount DESC, gd.userRating DESC NULLS LAST")
    List<GeneratedDiagram> findPopularPublicDiagrams(Pageable pageable);

    /**
     * Find diagrams by tags
     */
    @Query(value = """
        SELECT DISTINCT gd.* FROM generated_diagrams gd
        INNER JOIN diagram_tags dt ON gd.id = dt.diagram_id
        WHERE dt.tag = ANY(CAST(:tags AS text[]))
        AND gd.is_active = true
        AND (:isPublicOnly = false OR gd.is_public = true)
        ORDER BY gd.created_at DESC
        """, nativeQuery = true)
    List<GeneratedDiagram> findByTags(@Param("tags") String[] tags, @Param("isPublicOnly") boolean isPublicOnly);

    /**
     * Find highly rated diagrams
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.userRating >= :minRating " +
           "AND gd.isActive = true AND (:isPublicOnly = false OR gd.isPublic = true) " +
           "ORDER BY gd.userRating DESC, gd.viewCount DESC")
    List<GeneratedDiagram> findHighRatedDiagrams(@Param("minRating") Integer minRating, 
                                                  @Param("isPublicOnly") boolean isPublicOnly);

    /**
     * Find diagrams created in date range
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.createdAt BETWEEN :startDate AND :endDate " +
           "AND gd.isActive = true ORDER BY gd.createdAt DESC")
    List<GeneratedDiagram> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get diagram statistics by user
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_diagrams,
            COUNT(CASE WHEN is_public = true THEN 1 END) as public_diagrams,
            SUM(view_count) as total_views,
            SUM(download_count) as total_downloads,
            AVG(user_rating) as avg_rating,
            AVG(generation_time_ms) as avg_generation_time
        FROM generated_diagrams 
        WHERE generated_by = :userId AND is_active = true
        """, nativeQuery = true)
    Object getDiagramStatisticsByUser(@Param("userId") UUID userId);

    /**
     * Find diagrams by LLM model used
     */
    List<GeneratedDiagram> findByLlmModelUsedAndIsActiveTrueOrderByCreatedAtDesc(String llmModelUsed);

    /**
     * Find recently viewed diagrams
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.viewCount > 0 AND gd.isActive = true " +
           "ORDER BY gd.updatedAt DESC")
    List<GeneratedDiagram> findRecentlyViewedDiagrams(Pageable pageable);

    /**
     * Find diagrams with generation time above threshold (for performance analysis)
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.generationTimeMs > :thresholdMs " +
           "AND gd.isActive = true ORDER BY gd.generationTimeMs DESC")
    List<GeneratedDiagram> findSlowGenerationDiagrams(@Param("thresholdMs") Long thresholdMs);

    /**
     * Count diagrams by type and user
     */
    @Query("SELECT gd.diagramType, COUNT(gd) FROM GeneratedDiagram gd " +
           "WHERE gd.generatedBy = :userId AND gd.isActive = true GROUP BY gd.diagramType")
    List<Object[]> countDiagramsByTypeForUser(@Param("userId") UUID userId);

    /**
     * Find diagrams without ratings (for feedback requests)
     */
    @Query("SELECT gd FROM GeneratedDiagram gd WHERE gd.userRating IS NULL " +
           "AND gd.generatedBy = :userId AND gd.isActive = true " +
           "ORDER BY gd.createdAt DESC")
    List<GeneratedDiagram> findUnratedDiagramsByUser(@Param("userId") UUID userId);

    /**
     * Get popular tags from diagrams
     */
    @Query(value = """
        SELECT dt.tag, COUNT(*) as usage_count
        FROM diagram_tags dt
        INNER JOIN generated_diagrams gd ON dt.diagram_id = gd.id
        WHERE gd.is_active = true
        AND (:isPublicOnly = false OR gd.is_public = true)
        GROUP BY dt.tag
        ORDER BY usage_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPopularTags(@Param("isPublicOnly") boolean isPublicOnly, @Param("limit") Integer limit);

    /**
     * Find similar diagrams by source document domain tags
     */
    @Query(value = """
        SELECT DISTINCT gd.* FROM generated_diagrams gd
        INNER JOIN ai_documents d ON gd.source_document_id = d.id
        INNER JOIN document_domain_tags ddt ON d.id = ddt.document_id
        WHERE ddt.tag = ANY(CAST(:domainTags AS text[]))
        AND gd.id != :excludeDiagramId
        AND gd.is_active = true
        AND gd.is_public = true
        ORDER BY gd.view_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<GeneratedDiagram> findSimilarDiagrams(@Param("domainTags") String[] domainTags, 
                                               @Param("excludeDiagramId") UUID excludeDiagramId,
                                               @Param("limit") Integer limit);

    /**
     * Delete diagrams by source document (cascade cleanup)
     */
    void deleteBySourceDocumentId(UUID sourceDocumentId);
}
