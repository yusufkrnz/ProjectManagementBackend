package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for Canvas File operations
 * Handles file uploads, storage, and management for canvas boards
 */
public interface CanvasFileService {

    // File upload operations
    CanvasFile uploadFile(UUID canvasBoardId, MultipartFile file, UUID userId);

    CanvasFile uploadFileFromUrl(UUID canvasBoardId, String fileUrl, String filename, UUID userId);

    CanvasFile uploadFileFromStream(UUID canvasBoardId, InputStream inputStream,
                                  String filename, String mimeType, Long fileSize, UUID userId);

    // File retrieval
    CanvasFile getFile(UUID fileId, UUID userId);
    
    CanvasFile getFileByExcalidrawId(UUID canvasBoardId, String excalidrawFileId, UUID userId);
    
    List<CanvasFile> getCanvasFiles(UUID canvasBoardId, UUID userId);
    
    List<CanvasFile> getCanvasImages(UUID canvasBoardId, UUID userId);

    // File content operations
    byte[] getFileContent(UUID fileId, UUID userId);

    InputStream getFileStream(UUID fileId, UUID userId);
    
    String getFileUrl(UUID fileId);
    
    String getPublicFileUrl(UUID fileId);

    // Thumbnail operations
    CanvasFile generateThumbnail(UUID fileId, UUID userId);
    
    byte[] getThumbnailContent(UUID fileId, UUID userId);
    
    String getThumbnailUrl(UUID fileId);

    // File metadata operations
    CanvasFile updateFileMetadata(UUID fileId, String originalFilename, UUID userId);
    
    CanvasFile updateCanvasPosition(UUID fileId, Double x, Double y, Double width, Double height, UUID userId);
    
    CanvasFile setExcalidrawFileId(UUID fileId, String excalidrawFileId, UUID userId);

    // File management
    void deleteFile(UUID fileId, UUID userId);
    
    void deleteCanvasFiles(UUID canvasBoardId, UUID userId);
    
    CanvasFile duplicateFile(UUID fileId, UUID targetCanvasBoardId, UUID userId);

    // File validation and processing
    boolean isValidFileType(String mimeType);
    
    boolean isImageFile(String mimeType);
    
    boolean isFileSizeValid(Long fileSize);
    
    void validateFileAccess(UUID fileId, UUID userId);

    // Storage and cleanup operations
    void processUploadedFile(UUID fileId);
    
    void cleanupUnusedFiles(UUID canvasBoardId);
    
    void cleanupOldFiles(int daysOld);

    // File statistics
    Long getTotalFileSize(UUID canvasBoardId);
    
    Long getTotalWorkspaceFileSize(UUID workspaceId);
    
    Long getFileCount(UUID canvasBoardId);
    
    List<CanvasFile> getMostUsedFiles(UUID canvasBoardId, int limit);
    
    List<CanvasFile> getRecentFiles(UUID canvasBoardId, int limit);

    // File search and filtering
    List<CanvasFile> searchFilesByName(UUID canvasBoardId, String searchTerm, UUID userId);
    
    List<CanvasFile> getFilesByType(UUID canvasBoardId, String mimeTypePrefix, UUID userId);
    
    List<CanvasFile> getFilesBySizeRange(UUID canvasBoardId, Long minSize, Long maxSize, UUID userId);

    // Duplicate detection
    CanvasFile findDuplicateFile(String fileHash, UUID canvasBoardId);
    
    List<CanvasFile> findDuplicateFiles(UUID canvasBoardId);
    
    String calculateFileHash(MultipartFile file);

    // File conversion and processing
    CanvasFile convertToWebFormat(UUID fileId, UUID userId);
    
    CanvasFile optimizeImage(UUID fileId, Integer maxWidth, Integer maxHeight, UUID userId);
    
    CanvasFile compressFile(UUID fileId, UUID userId);

    // Usage tracking
    void incrementUsageCount(UUID fileId);
    
    void trackFileAccess(UUID fileId, UUID userId);

    // Batch operations
    List<CanvasFile> uploadMultipleFiles(UUID canvasBoardId, List<MultipartFile> files, UUID userId);
    
    void deleteMultipleFiles(List<UUID> fileIds, UUID userId);
    
    List<CanvasFile> duplicateFiles(List<UUID> fileIds, UUID targetCanvasBoardId, UUID userId);
}
