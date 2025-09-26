package com.yusufkurnaz.ProjectManagementBackend.IdeaWorkspace.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canvas File entity for storing uploaded files (images, documents)
 * Used in canvas boards (Excalidraw files section)
 */
@Entity
@Table(name = "canvas_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CanvasFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canvas_board_id", nullable = false)
    private CanvasBoard canvasBoard;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath; // Relative or absolute path

    @Column(name = "file_url", length = 1000)
    private String fileUrl; // Public accessible URL

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // in bytes

    @Column(name = "file_hash", length = 64)
    private String fileHash; // SHA-256 for deduplication

    // Image-specific metadata
    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "has_thumbnail")
    @Builder.Default
    private Boolean hasThumbnail = false;

    @Column(name = "thumbnail_path", length = 1000)
    private String thumbnailPath;

    // Canvas-specific metadata
    @Column(name = "excalidraw_file_id", length = 100)
    private String excalidrawFileId; // ID used in Excalidraw files object

    @Column(name = "canvas_position_x")
    private Double canvasPositionX;

    @Column(name = "canvas_position_y")
    private Double canvasPositionY;

    @Column(name = "canvas_width")
    private Double canvasWidth;

    @Column(name = "canvas_height")
    private Double canvasHeight;

    // Usage tracking
    @Column(name = "usage_count")
    @Builder.Default
    private Long usageCount = 0L;

    @Column(name = "last_used_at")
    private java.time.LocalDateTime lastUsedAt;

    // File status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private FileStatus status = FileStatus.UPLOADED;

    public enum FileStatus {
        UPLOADING,
        UPLOADED,
        PROCESSING,
        READY,
        ERROR,
        DELETED
    }

    // Helper methods
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0L) + 1;
        this.lastUsedAt = java.time.LocalDateTime.now();
    }

    public boolean isImage() {
        return this.mimeType != null && this.mimeType.startsWith("image/");
    }

    public boolean isReady() {
        return FileStatus.READY.equals(this.status);
    }

    public String getFileExtension() {
        if (this.originalFilename != null && this.originalFilename.contains(".")) {
            return this.originalFilename.substring(this.originalFilename.lastIndexOf("."));
        }
        return "";
    }

    // Generate public URL (can be overridden by service)
    public String generatePublicUrl(String baseUrl) {
        return baseUrl + "/api/v1/canvas/files/" + this.getId();
    }
}
