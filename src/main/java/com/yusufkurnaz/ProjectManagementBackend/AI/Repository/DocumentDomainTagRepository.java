package com.yusufkurnaz.ProjectManagementBackend.AI.Repository;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentDomainTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for DocumentDomainTag entity
 * Manages document tagging and domain categorization
 */
@Repository
public interface DocumentDomainTagRepository extends JpaRepository<DocumentDomainTag, UUID> {

    // Find tags by document
    List<DocumentDomainTag> findByDocumentIdAndIsActiveTrue(UUID documentId);

    // Find documents by tag
    @Query("SELECT ddt.document FROM DocumentDomainTag ddt WHERE ddt.tag = :tag AND ddt.isActive = true")
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findDocumentsByTag(@Param("tag") String tag);

    // Find documents by multiple tags (OR condition)
    @Query("SELECT DISTINCT ddt.document FROM DocumentDomainTag ddt WHERE ddt.tag IN :tags AND ddt.isActive = true")
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findDocumentsByTagsIn(@Param("tags") List<String> tags);

    // Find documents by multiple tags (AND condition - all tags must be present)
    @Query("SELECT d FROM Document d WHERE d.id IN " +
           "(SELECT ddt.document.id FROM DocumentDomainTag ddt WHERE ddt.tag IN :tags AND ddt.isActive = true " +
           "GROUP BY ddt.document.id HAVING COUNT(DISTINCT ddt.tag) = :tagCount)")
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findDocumentsByAllTags(@Param("tags") List<String> tags, @Param("tagCount") long tagCount);

    // Find popular tags
    @Query("SELECT ddt.tag, COUNT(ddt) as tagCount FROM DocumentDomainTag ddt " +
           "WHERE ddt.isActive = true GROUP BY ddt.tag ORDER BY tagCount DESC")
    List<Object[]> findPopularTags();

    // Find popular tags with limit
    @Query("SELECT ddt.tag, COUNT(ddt) as tagCount FROM DocumentDomainTag ddt " +
           "WHERE ddt.isActive = true GROUP BY ddt.tag ORDER BY tagCount DESC")
    List<Object[]> findPopularTags(org.springframework.data.domain.Pageable pageable);

    // Find tags by user
    @Query("SELECT ddt FROM DocumentDomainTag ddt WHERE ddt.taggedBy = :userId AND ddt.isActive = true")
    List<DocumentDomainTag> findTagsByUser(@Param("userId") UUID userId);

    // Find AI-generated tags
    @Query("SELECT ddt FROM DocumentDomainTag ddt WHERE ddt.tagSource = 'AI_GENERATED' AND ddt.isActive = true")
    List<DocumentDomainTag> findAiGeneratedTags();

    // Find high-confidence tags
    @Query("SELECT ddt FROM DocumentDomainTag ddt WHERE ddt.confidenceScore >= :minConfidence AND ddt.isActive = true")
    List<DocumentDomainTag> findHighConfidenceTags(@Param("minConfidence") Float minConfidence);

    // Check if document has specific tag
    @Query("SELECT COUNT(ddt) > 0 FROM DocumentDomainTag ddt WHERE ddt.document.id = :documentId AND ddt.tag = :tag AND ddt.isActive = true")
    boolean documentHasTag(@Param("documentId") UUID documentId, @Param("tag") String tag);

    // Find related tags (tags that often appear together)
    @Query("SELECT ddt2.tag, COUNT(ddt2) as coOccurrence FROM DocumentDomainTag ddt1 " +
           "JOIN DocumentDomainTag ddt2 ON ddt1.document.id = ddt2.document.id " +
           "WHERE ddt1.tag = :tag AND ddt2.tag != :tag AND ddt1.isActive = true AND ddt2.isActive = true " +
           "GROUP BY ddt2.tag ORDER BY coOccurrence DESC")
    List<Object[]> findRelatedTags(@Param("tag") String tag);

    // Find documents for similarity search (with specific tags)
    @Query("SELECT DISTINCT ddt.document FROM DocumentDomainTag ddt WHERE ddt.tag IN :tags AND ddt.isActive = true " +
           "AND ddt.document.isActive = true ORDER BY ddt.document.createdAt DESC")
    List<com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document> findDocumentsForSimilaritySearch(@Param("tags") List<String> tags);

    // Count documents by tag
    @Query("SELECT COUNT(DISTINCT ddt.document) FROM DocumentDomainTag ddt WHERE ddt.tag = :tag AND ddt.isActive = true")
    Long countDocumentsByTag(@Param("tag") String tag);

    // Find user's domain preferences (most used tags by user)
    @Query("SELECT ddt.tag, COUNT(ddt) as usage FROM DocumentDomainTag ddt " +
           "WHERE ddt.taggedBy = :userId AND ddt.isActive = true " +
           "GROUP BY ddt.tag ORDER BY usage DESC")
    List<Object[]> findUserDomainPreferences(@Param("userId") UUID userId);

    // Find trending tags (recently active)
    @Query("SELECT ddt.tag, COUNT(ddt) as recentUsage FROM DocumentDomainTag ddt " +
           "WHERE ddt.createdAt >= :since AND ddt.isActive = true " +
           "GROUP BY ddt.tag ORDER BY recentUsage DESC")
    List<Object[]> findTrendingTags(@Param("since") java.time.LocalDateTime since);

    // Delete tags by document (for cleanup)
    void deleteByDocumentIdAndIsActiveTrue(UUID documentId);

    // Find tags for recommendation system
    @Query("SELECT DISTINCT ddt.tag FROM DocumentDomainTag ddt " +
           "JOIN DocumentChunk dc ON ddt.document.id = dc.document.id " +
           "WHERE dc.embedding IS NOT NULL AND ddt.isActive = true")
    List<String> findTagsWithEmbeddings();
}
