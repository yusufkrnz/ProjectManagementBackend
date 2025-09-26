package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasFile;
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
 * Repository for CanvasFile entity
 * Manages file uploads and metadata for canvas boards
 */
@Repository
public interface CanvasFileRepository extends JpaRepository<CanvasFile, UUID> {

    // Find by canvas board
    List<CanvasFile> findByCanvasBoardIdAndIsActiveTrue(UUID canvasBoardId);

    // Find by file properties
    Optional<CanvasFile> findByFileHashAndIsActiveTrue(String fileHash);
    
    List<CanvasFile> findByMimeTypeStartingWithAndIsActiveTrue(String mimeTypePrefix);

    // Find by Excalidraw file ID
    Optional<CanvasFile> findByExcalidrawFileIdAndCanvasBoardIdAndIsActiveTrue(
        String excalidrawFileId, UUID canvasBoardId);

    // Find by status
    List<CanvasFile> findByStatusAndIsActiveTrue(CanvasFile.FileStatus status);
    
    List<CanvasFile> findByCanvasBoardIdAndStatusAndIsActiveTrue(
        UUID canvasBoardId, CanvasFile.FileStatus status);

    // File size and storage queries
    @Query("SELECT SUM(cf.fileSize) FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND cf.isActive = true")
    Long getTotalFileSizeByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId);

    @Query("SELECT SUM(cf.fileSize) FROM CanvasFile cf WHERE cf.canvasBoard.workspace.id = :workspaceId AND cf.isActive = true")
    Long getTotalFileSizeByWorkspace(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(cf) FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND cf.isActive = true")
    Long countFilesByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId);

    // Image-specific queries
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND " +
           "cf.mimeType LIKE 'image/%' AND cf.isActive = true")
    List<CanvasFile> findImagesByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId);

    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND " +
           "cf.mimeType LIKE 'image/%' AND cf.hasThumbnail = true AND cf.isActive = true")
    List<CanvasFile> findImagesWithThumbnailsByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId);

    // Usage tracking
    @Modifying
    @Query("UPDATE CanvasFile cf SET cf.usageCount = cf.usageCount + 1, " +
           "cf.lastUsedAt = :usedAt WHERE cf.id = :fileId")
    void incrementUsageCount(@Param("fileId") UUID fileId, @Param("usedAt") LocalDateTime usedAt);

    // Most used files
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND " +
           "cf.isActive = true ORDER BY cf.usageCount DESC")
    List<CanvasFile> findMostUsedFilesByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId, 
                                                   org.springframework.data.domain.Pageable pageable);

    // Recently uploaded
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND " +
           "cf.isActive = true ORDER BY cf.createdAt DESC")
    List<CanvasFile> findRecentFilesByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId,
                                                 org.springframework.data.domain.Pageable pageable);

    // Unused files (for cleanup)
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.isActive = true AND " +
           "cf.usageCount = 0 AND cf.createdAt < :cutoffDate")
    List<CanvasFile> findUnusedFiles(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Files without thumbnails (for processing)
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.isActive = true AND " +
           "cf.mimeType LIKE 'image/%' AND cf.hasThumbnail = false AND " +
           "cf.status = 'UPLOADED'")
    List<CanvasFile> findImagesWithoutThumbnails();

    // Duplicate files by hash
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.fileHash = :fileHash AND " +
           "cf.isActive = true AND cf.id != :excludeFileId")
    List<CanvasFile> findDuplicatesByHash(@Param("fileHash") String fileHash, 
                                        @Param("excludeFileId") UUID excludeFileId);

    // Files by size range
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND " +
           "cf.isActive = true AND cf.fileSize BETWEEN :minSize AND :maxSize")
    List<CanvasFile> findFilesBySizeRange(@Param("canvasBoardId") UUID canvasBoardId,
                                        @Param("minSize") Long minSize,
                                        @Param("maxSize") Long maxSize);

    // Search files by name
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.id = :canvasBoardId AND " +
           "cf.isActive = true AND LOWER(cf.originalFilename) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<CanvasFile> searchFilesByName(@Param("canvasBoardId") UUID canvasBoardId,
                                     @Param("searchTerm") String searchTerm);

    // Files by workspace
    @Query("SELECT cf FROM CanvasFile cf WHERE cf.canvasBoard.workspace.id = :workspaceId AND cf.isActive = true")
    List<CanvasFile> findFilesByWorkspace(@Param("workspaceId") UUID workspaceId);

    // Cleanup operations
    @Modifying
    @Query("UPDATE CanvasFile cf SET cf.isActive = false WHERE cf.canvasBoard.id = :canvasBoardId")
    void softDeleteFilesByCanvasBoard(@Param("canvasBoardId") UUID canvasBoardId);

    @Modifying
    @Query("DELETE FROM CanvasFile cf WHERE cf.isActive = false AND cf.updatedAt < :cutoffDate")
    void hardDeleteInactiveFiles(@Param("cutoffDate") LocalDateTime cutoffDate);
}
