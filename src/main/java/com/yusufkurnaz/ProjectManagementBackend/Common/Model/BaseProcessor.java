package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all processing operations (PDF, Document, etc.)
 * Follows SRP - Single Responsibility: Managing processing lifecycle
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public abstract class BaseProcessor extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "processed_by")
    private UUID processedBy; // User who initiated the process

    @Column(name = "processing_metadata", columnDefinition = "jsonb")
    private String processingMetadata; // JSON for flexible metadata

    // Constructor
    public BaseProcessor() {
        super();
        this.processingStatus = ProcessingStatus.PENDING;
        this.progressPercentage = 0;
    }

    /**
     * Mark processing as started
     */
    public void startProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
        this.progressPercentage = 0;
    }

    /**
     * Update processing progress
     */
    public void updateProgress(Integer percentage) {
        this.progressPercentage = Math.min(100, Math.max(0, percentage));
    }

    /**
     * Mark processing as completed successfully
     */
    public void completeProcessing() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
    }

    /**
     * Mark processing as failed
     */
    public void failProcessing(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Check if processing is in progress
     */
    public boolean isProcessing() {
        return ProcessingStatus.PROCESSING.equals(this.processingStatus);
    }

    /**
     * Check if processing is completed
     */
    public boolean isCompleted() {
        return ProcessingStatus.COMPLETED.equals(this.processingStatus);
    }

    /**
     * Check if processing has failed
     */
    public boolean hasFailed() {
        return ProcessingStatus.FAILED.equals(this.processingStatus);
    }
}
