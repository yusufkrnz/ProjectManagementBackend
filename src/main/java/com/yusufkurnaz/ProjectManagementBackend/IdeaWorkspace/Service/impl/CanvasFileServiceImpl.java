package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasBoard;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity.CanvasFile;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository.CanvasBoardRepository;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Repository.CanvasFileRepository;
import com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Service.CanvasFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of CanvasFileService
 * Handles file upload, storage, and management for canvas boards
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CanvasFileServiceImpl implements CanvasFileService {

    private final CanvasFileRepository canvasFileRepository;
    private final CanvasBoardRepository canvasBoardRepository;

    @Value("${app.file.upload-dir:./uploads/canvas}")
    private String uploadDir;

    @Value("${app.file.max-size:10485760}") // 10MB
    private Long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "text/plain", "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Override
    public CanvasFile uploadFile(UUID canvasBoardId, MultipartFile file, UUID userId) {
        log.info("Uploading file '{}' to canvas {} by user {}", file.getOriginalFilename(), canvasBoardId, userId);

        // Validate canvas access
        validateCanvasAccess(canvasBoardId, userId);

        // Validate file
        validateFile(file);

        // Check for duplicates
        String fileHash = calculateFileHash(file);
        Optional<CanvasFile> existingFile = canvasFileRepository.findByFileHashAndIsActiveTrue(fileHash);
        if (existingFile.isPresent()) {
            log.info("Duplicate file detected, returning existing file: {}", existingFile.get().getId());
            return existingFile.get();
        }

        // Save file to disk
        String storedFilename = generateUniqueFilename(file.getOriginalFilename());
        Path filePath;
        try {
            filePath = saveFileToDisk(file, storedFilename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file to disk", e);
        }

        // Create CanvasFile entity
        CanvasBoard canvas = canvasBoardRepository.findById(canvasBoardId)
                .orElseThrow(() -> new RuntimeException("Canvas not found: " + canvasBoardId));

        CanvasFile canvasFile = CanvasFile.builder()
                .canvasBoard(canvas)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .filePath(filePath.toString())
                .fileUrl(generateFileUrl(storedFilename))
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .fileHash(fileHash)
                .status(CanvasFile.FileStatus.UPLOADED)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        // Set image dimensions if it's an image
        if (isImageFile(file.getContentType())) {
            setImageDimensions(canvasFile, file);
        }

        CanvasFile savedFile = canvasFileRepository.save(canvasFile);
        log.info("File uploaded successfully with ID: {}", savedFile.getId());

        return savedFile;
    }

    @Override
    public CanvasFile uploadFileFromUrl(UUID canvasBoardId, String fileUrl, String filename, UUID userId) {
        // TODO: Implement URL-based file upload
        throw new UnsupportedOperationException("URL-based upload not implemented yet");
    }

    @Override
    public CanvasFile uploadFileFromStream(UUID canvasBoardId, InputStream inputStream, 
                                         String filename, String mimeType, Long fileSize, UUID userId) {
        // TODO: Implement stream-based file upload
        throw new UnsupportedOperationException("Stream-based upload not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public CanvasFile getFile(UUID fileId, UUID userId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        validateFileAccess(fileId, userId);
        
        // Update usage tracking
        file.incrementUsageCount();
        file.setLastUsedAt(LocalDateTime.now());
        canvasFileRepository.save(file);

        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public CanvasFile getFileByExcalidrawId(UUID canvasBoardId, String excalidrawFileId, UUID userId) {
        return canvasFileRepository.findByExcalidrawFileIdAndCanvasBoardIdAndIsActiveTrue(excalidrawFileId, canvasBoardId)
                .orElseThrow(() -> new RuntimeException("File not found with Excalidraw ID: " + excalidrawFileId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> getCanvasFiles(UUID canvasBoardId, UUID userId) {
        validateCanvasAccess(canvasBoardId, userId);
        return canvasFileRepository.findByCanvasBoardIdAndIsActiveTrue(canvasBoardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> getCanvasImages(UUID canvasBoardId, UUID userId) {
        validateCanvasAccess(canvasBoardId, userId);
        return canvasFileRepository.findImagesByCanvasBoard(canvasBoardId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getFileContent(UUID fileId, UUID userId) {
        CanvasFile file = getFile(fileId, userId);
        Path filePath = Paths.get(file.getFilePath());
        
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk: " + file.getFilePath());
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getFileStream(UUID fileId, UUID userId) {
        CanvasFile file = getFile(fileId, userId);
        Path filePath = Paths.get(file.getFilePath());
        
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk: " + file.getFilePath());
        }

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open file stream", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getFileUrl(UUID fileId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        return file.getFileUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public String getPublicFileUrl(UUID fileId) {
        // TODO: Implement public URL generation (with expiry tokens)
        return getFileUrl(fileId);
    }

    @Override
    public CanvasFile generateThumbnail(UUID fileId, UUID userId) {
        // TODO: Implement thumbnail generation
        throw new UnsupportedOperationException("Thumbnail generation not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getThumbnailContent(UUID fileId, UUID userId) {
        // TODO: Implement thumbnail content retrieval
        throw new UnsupportedOperationException("Thumbnail retrieval not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public String getThumbnailUrl(UUID fileId) {
        // TODO: Implement thumbnail URL generation
        throw new UnsupportedOperationException("Thumbnail URL not implemented yet");
    }

    @Override
    public CanvasFile updateFileMetadata(UUID fileId, String originalFilename, UUID userId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        validateFileAccess(fileId, userId);

        file.setOriginalFilename(originalFilename);
        file.setUpdatedBy(userId);
        
        return canvasFileRepository.save(file);
    }

    @Override
    public CanvasFile updateCanvasPosition(UUID fileId, Double x, Double y, Double width, Double height, UUID userId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        validateFileAccess(fileId, userId);

        file.setCanvasPositionX(x);
        file.setCanvasPositionY(y);
        file.setCanvasWidth(width);
        file.setCanvasHeight(height);
        file.setUpdatedBy(userId);
        
        return canvasFileRepository.save(file);
    }

    @Override
    public CanvasFile setExcalidrawFileId(UUID fileId, String excalidrawFileId, UUID userId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        validateFileAccess(fileId, userId);

        file.setExcalidrawFileId(excalidrawFileId);
        file.setUpdatedBy(userId);
        
        return canvasFileRepository.save(file);
    }

    @Override
    public void deleteFile(UUID fileId, UUID userId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        validateFileAccess(fileId, userId);

        // Soft delete
        file.setIsActive(false);
        file.setStatus(CanvasFile.FileStatus.DELETED);
        file.setUpdatedBy(userId);
        canvasFileRepository.save(file);

        log.info("File {} soft deleted by user {}", fileId, userId);
    }

    @Override
    public void deleteCanvasFiles(UUID canvasBoardId, UUID userId) {
        validateCanvasAccess(canvasBoardId, userId);
        
        List<CanvasFile> files = canvasFileRepository.findByCanvasBoardIdAndIsActiveTrue(canvasBoardId);
        
        files.forEach(file -> {
            file.setIsActive(false);
            file.setStatus(CanvasFile.FileStatus.DELETED);
            file.setUpdatedBy(userId);
        });

        canvasFileRepository.saveAll(files);
        log.info("All files for canvas {} deleted by user {}", canvasBoardId, userId);
    }

    @Override
    public CanvasFile duplicateFile(UUID fileId, UUID targetCanvasBoardId, UUID userId) {
        // TODO: Implement file duplication
        throw new UnsupportedOperationException("File duplication not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidFileType(String mimeType) {
        return ALLOWED_IMAGE_TYPES.contains(mimeType) || ALLOWED_DOCUMENT_TYPES.contains(mimeType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isImageFile(String mimeType) {
        return ALLOWED_IMAGE_TYPES.contains(mimeType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFileSizeValid(Long fileSize) {
        return fileSize != null && fileSize <= maxFileSize;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateFileAccess(UUID fileId, UUID userId) {
        CanvasFile file = canvasFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        
        validateCanvasAccess(file.getCanvasBoard().getId(), userId);
    }

    @Override
    public void processUploadedFile(UUID fileId) {
        // TODO: Implement async file processing (virus scan, optimization, etc.)
    }

    @Override
    public void cleanupUnusedFiles(UUID canvasBoardId) {
        // TODO: Implement cleanup of unused files
    }

    @Override
    public void cleanupOldFiles(int daysOld) {
        // TODO: Implement cleanup of old deleted files
    }

    // Additional interface methods implementation...

    @Override
    @Transactional(readOnly = true)
    public Long getTotalFileSize(UUID canvasBoardId) {
        return canvasFileRepository.getTotalFileSizeByCanvasBoard(canvasBoardId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalWorkspaceFileSize(UUID workspaceId) {
        return canvasFileRepository.getTotalFileSizeByWorkspace(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getFileCount(UUID canvasBoardId) {
        return canvasFileRepository.countFilesByCanvasBoard(canvasBoardId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> getMostUsedFiles(UUID canvasBoardId, int limit) {
        return canvasFileRepository.findMostUsedFilesByCanvasBoard(canvasBoardId, 
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> getRecentFiles(UUID canvasBoardId, int limit) {
        return canvasFileRepository.findRecentFilesByCanvasBoard(canvasBoardId, 
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> searchFilesByName(UUID canvasBoardId, String searchTerm, UUID userId) {
        validateCanvasAccess(canvasBoardId, userId);
        return canvasFileRepository.searchFilesByName(canvasBoardId, searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> getFilesByType(UUID canvasBoardId, String mimeTypePrefix, UUID userId) {
        validateCanvasAccess(canvasBoardId, userId);
        if ("image/".equals(mimeTypePrefix)) {
            return canvasFileRepository.findImagesByCanvasBoard(canvasBoardId);
        } else {
            return canvasFileRepository.findByMimeTypeStartingWithAndIsActiveTrue(mimeTypePrefix);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> getFilesBySizeRange(UUID canvasBoardId, Long minSize, Long maxSize, UUID userId) {
        validateCanvasAccess(canvasBoardId, userId);
        return canvasFileRepository.findFilesBySizeRange(canvasBoardId, minSize, maxSize);
    }

    @Override
    @Transactional(readOnly = true)
    public CanvasFile findDuplicateFile(String fileHash, UUID canvasBoardId) {
        Optional<CanvasFile> file = canvasFileRepository.findByFileHashAndIsActiveTrue(fileHash);
        return file.orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanvasFile> findDuplicateFiles(UUID canvasBoardId) {
        // For now, return empty list. This needs a complex query.
        return List.of();
    }

    @Override
    public String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating file hash", e);
        }
    }

    // Additional methods continue with similar implementations...
    // (Other interface methods would be implemented here)

    // Private helper methods
    private void validateCanvasAccess(UUID canvasBoardId, UUID userId) {
        if (!canvasBoardRepository.findByIdWithAccess(canvasBoardId, userId).isPresent()) {
            throw new RuntimeException("Access denied to canvas: " + canvasBoardId);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (!isValidFileType(file.getContentType())) {
            throw new RuntimeException("Invalid file type: " + file.getContentType());
        }

        if (!isFileSizeValid(file.getSize())) {
            throw new RuntimeException("File size exceeds limit: " + file.getSize());
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    private Path saveFileToDisk(MultipartFile file, String filename) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath;
    }

    private String generateFileUrl(String filename) {
        return "/api/v1/canvas/files/" + filename;
    }

    private void setImageDimensions(CanvasFile canvasFile, MultipartFile file) {
        // TODO: Implement image dimension detection
        // For now, set default dimensions
        canvasFile.setImageWidth(800);
        canvasFile.setImageHeight(600);
    }

    // Implement remaining interface methods with similar patterns...
    @Override
    public CanvasFile convertToWebFormat(UUID fileId, UUID userId) {
        throw new UnsupportedOperationException("Web format conversion not implemented yet");
    }

    @Override
    public CanvasFile optimizeImage(UUID fileId, Integer maxWidth, Integer maxHeight, UUID userId) {
        throw new UnsupportedOperationException("Image optimization not implemented yet");
    }

    @Override
    public CanvasFile compressFile(UUID fileId, UUID userId) {
        throw new UnsupportedOperationException("File compression not implemented yet");
    }

    @Override
    public void incrementUsageCount(UUID fileId) {
        canvasFileRepository.incrementUsageCount(fileId, LocalDateTime.now());
    }

    @Override
    public void trackFileAccess(UUID fileId, UUID userId) {
        incrementUsageCount(fileId);
    }

    @Override
    public List<CanvasFile> uploadMultipleFiles(UUID canvasBoardId, List<MultipartFile> files, UUID userId) {
        return files.stream()
                .map(file -> uploadFile(canvasBoardId, file, userId))
                .toList();
    }

    @Override
    public void deleteMultipleFiles(List<UUID> fileIds, UUID userId) {
        fileIds.forEach(fileId -> deleteFile(fileId, userId));
    }

    @Override
    public List<CanvasFile> duplicateFiles(List<UUID> fileIds, UUID targetCanvasBoardId, UUID userId) {
        return fileIds.stream()
                .map(fileId -> duplicateFile(fileId, targetCanvasBoardId, userId))
                .toList();
    }
}
