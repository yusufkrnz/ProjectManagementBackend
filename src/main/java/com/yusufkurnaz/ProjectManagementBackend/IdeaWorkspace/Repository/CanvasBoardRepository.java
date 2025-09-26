package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CanvasBoard entity
 * Provides CRUD operations and custom queries for canvas management
 */
@Repository
public interface CanvasBoardRepository extends JpaRepository<CanvasBoard, UUID> {

    // Find by workspace
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.workspace.id = :workspaceId AND cb.isActive = true")
    List<CanvasBoard> findByWorkspaceIdAndIsActiveTrue(@Param("workspaceId") UUID workspaceId);
    
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.workspace.id = :workspaceId AND cb.isActive = true")
    Page<CanvasBoard> findByWorkspaceIdAndIsActiveTrue(@Param("workspaceId") UUID workspaceId, Pageable pageable);

    // Find by creator
    List<CanvasBoard> findByCreatedByAndIsActiveTrue(UUID createdBy);
    
    Page<CanvasBoard> findByCreatedByAndIsActiveTrue(UUID createdBy, Pageable pageable);

    // Find public canvases
    List<CanvasBoard> findByIsPublicTrueAndIsActiveTrue();
    
    Page<CanvasBoard> findByIsPublicTrueAndIsActiveTrue(Pageable pageable);

    // Find templates
    List<CanvasBoard> findByIsTemplateTrueAndIsActiveTrue();
    
    Page<CanvasBoard> findByIsTemplateTrueAndIsActiveTrue(Pageable pageable);

    // Search by name
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "LOWER(cb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<CanvasBoard> searchByName(@Param("searchTerm") String searchTerm);

    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "cb.workspace.id = :workspaceId AND " +
           "LOWER(cb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<CanvasBoard> searchByNameInWorkspace(@Param("workspaceId") UUID workspaceId, 
                                            @Param("searchTerm") String searchTerm);

    // Search by tags
    @Query("SELECT DISTINCT cb FROM CanvasBoard cb JOIN cb.tags t WHERE cb.isActive = true AND " +
           "LOWER(t) IN :tags")
    List<CanvasBoard> findByTagsIn(@Param("tags") List<String> tags);

    @Query("SELECT DISTINCT cb FROM CanvasBoard cb JOIN cb.tags t WHERE cb.isActive = true AND " +
           "cb.workspace.id = :workspaceId AND LOWER(t) IN :tags")
    List<CanvasBoard> findByTagsInWorkspace(@Param("workspaceId") UUID workspaceId, 
                                          @Param("tags") List<String> tags);

    // Recently accessed
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "cb.createdBy = :userId ORDER BY cb.lastAccessedAt DESC")
    List<CanvasBoard> findRecentlyAccessedByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "cb.workspace.id = :workspaceId ORDER BY cb.lastAccessedAt DESC")
    List<CanvasBoard> findRecentlyAccessedInWorkspace(@Param("workspaceId") UUID workspaceId, 
                                                    Pageable pageable);

    // Most viewed
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "cb.isPublic = true ORDER BY cb.viewCount DESC")
    List<CanvasBoard> findMostViewed(Pageable pageable);

    // Update operations
    @Modifying
    @Query("UPDATE CanvasBoard cb SET cb.viewCount = cb.viewCount + 1, " +
           "cb.lastAccessedAt = :accessTime WHERE cb.id = :canvasId")
    void incrementViewCount(@Param("canvasId") UUID canvasId, @Param("accessTime") LocalDateTime accessTime);

    @Modifying
    @Query("UPDATE CanvasBoard cb SET cb.forkCount = cb.forkCount + 1 WHERE cb.id = :canvasId")
    void incrementForkCount(@Param("canvasId") UUID canvasId);

    @Modifying
    @Query("UPDATE CanvasBoard cb SET cb.lastAccessedAt = :accessTime WHERE cb.id = :canvasId")
    void updateLastAccessed(@Param("canvasId") UUID canvasId, @Param("accessTime") LocalDateTime accessTime);

    // Statistics
    @Query("SELECT COUNT(cb) FROM CanvasBoard cb WHERE cb.isActive = true AND cb.workspace.id = :workspaceId")
    Long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(cb) FROM CanvasBoard cb WHERE cb.isActive = true AND cb.createdBy = :userId")
    Long countByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(cb) FROM CanvasBoard cb WHERE cb.isActive = true AND cb.isPublic = true")
    Long countPublicCanvases();

    // Advanced search
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "(:workspaceId IS NULL OR cb.workspace.id = :workspaceId) AND " +
           "(:searchTerm IS NULL OR LOWER(cb.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(cb.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:isPublic IS NULL OR cb.isPublic = :isPublic) AND " +
           "(:isTemplate IS NULL OR cb.isTemplate = :isTemplate)")
    Page<CanvasBoard> searchCanvases(@Param("workspaceId") UUID workspaceId,
                                   @Param("searchTerm") String searchTerm,
                                   @Param("isPublic") Boolean isPublic,
                                   @Param("isTemplate") Boolean isTemplate,
                                   Pageable pageable);

    // Find canvas with collaborator check
    @Query("SELECT cb FROM CanvasBoard cb LEFT JOIN CanvasCollaborator cc ON cb.id = cc.canvasBoard.id " +
           "WHERE cb.id = :canvasId AND cb.isActive = true AND " +
           "(cb.createdBy = :userId OR cc.user.id = :userId OR cb.isPublic = true)")
    Optional<CanvasBoard> findByIdWithAccess(@Param("canvasId") UUID canvasId, @Param("userId") UUID userId);

    // Cleanup old inactive canvases
    @Query("SELECT cb FROM CanvasBoard cb WHERE cb.isActive = true AND " +
           "cb.lastAccessedAt < :cutoffDate AND cb.viewCount = 0")
    List<CanvasBoard> findInactiveCanvases(@Param("cutoffDate") LocalDateTime cutoffDate);
}
