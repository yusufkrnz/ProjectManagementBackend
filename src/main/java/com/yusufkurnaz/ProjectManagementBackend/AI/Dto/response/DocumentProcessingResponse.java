package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.FileType;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for document processing operations
 * Follows SRP - Single responsibility: Processing status data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentProcessingResponse {

    private String documentId;
    
    private String originalFilename;
    
    private FileType fileType;
    
    private Long fileSize;
    
    private ProcessingStatus processingStatus;
    
    private Integer progressPercentage;
    
    private String errorMessage;
    
    private LocalDateTime uploadedAt;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    private Integer totalPages;
    
    private Integer totalChunks;
    
    private List<String> domainTags;
    
    private List<String> userTags;
    
    private Float qualityScore;
    
    private String languageCode;
    
    private String contentHash;
    
    // Processing statistics
    private Long processingTimeMs;
    
    private String extractedTextPreview; // First 200 characters
    
    private List<String> suggestedDomainTags;
    
    private List<DiagramGenerationResponse> availableDiagrams;
    
    /**
     * Create response for upload confirmation
     */
    public static DocumentProcessingResponse uploadConfirmation(String documentId, String filename, ProcessingStatus status) {
        return DocumentProcessingResponse.builder()
                .documentId(documentId)
                .originalFilename(filename)
                .processingStatus(status)
                .progressPercentage(0)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create response for processing status
     */
    public static DocumentProcessingResponse statusUpdate(String documentId, ProcessingStatus status, Integer progress, String error) {
        return DocumentProcessingResponse.builder()
                .documentId(documentId)
                .processingStatus(status)
                .progressPercentage(progress)
                .errorMessage(error)
                .build();
    }
    
    /**
     * Check if processing is completed successfully
     */
    public boolean isProcessingCompleted() {
        return ProcessingStatus.COMPLETED.equals(processingStatus);
    }
    
    /**
     * Check if processing has failed
     */
    public boolean isProcessingFailed() {
        return ProcessingStatus.FAILED.equals(processingStatus);
    }
    
    /**
     * Get processing duration if completed
     */
    public Long getProcessingDurationMs() {
        if (startedAt != null && completedAt != null) {
            return java.time.Duration.between(startedAt, completedAt).toMillis();
        }
        return null;
    }
}
