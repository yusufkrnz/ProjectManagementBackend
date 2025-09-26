package com.yusufkurnaz.ProjectManagementBackend.AI.Controller;

import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request.DocumentUploadRequest;
import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response.DocumentProcessingResponse;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DocumentProcessingService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for document processing operations
 * Follows SRP - Single responsibility: HTTP request handling for documents
 */
@RestController
@RequestMapping("/api/v1/ai/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Document Controller", description = "Document upload and processing operations")
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document for processing", 
               description = "Upload PDF/DOCX file for text extraction and AI processing")
    public ResponseEntity<ApiResponse<DocumentProcessingResponse>> uploadDocument(
            @Parameter(description = "Document file to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Upload parameters") @Valid @ModelAttribute DocumentUploadRequest request,
            Authentication authentication) {
        
        log.info("Uploading document: {} for user: {}", file.getOriginalFilename(), authentication.getName());
        
        try {
            // Validate file
            documentProcessingService.validateFile(file);
            
            UUID userId = UUID.fromString(authentication.getName());
            
            // Process document
            Document document = documentProcessingService.processDocument(
                    file, 
                    userId, 
                    request.getCleanUserTags()
            );
            
            // Add domain tags if provided
            if (!request.getCleanDomainTags().isEmpty()) {
                document = documentProcessingService.addDomainTags(
                        document.getId(), 
                        request.getCleanDomainTags()
                );
            }
            
            DocumentProcessingResponse response = mapToResponse(document);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Document uploaded and processing started"));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid file: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/status")
    @Operation(summary = "Get document processing status", 
               description = "Check the processing status of an uploaded document")
    public ResponseEntity<ApiResponse<DocumentProcessingResponse>> getDocumentStatus(
            @Parameter(description = "Document ID") @PathVariable String documentId,
            Authentication authentication) {
        
        try {
            UUID docId = UUID.fromString(documentId);
            Document document = documentProcessingService.getDocumentStatus(docId);
            
            // Check if user has access to this document
            UUID userId = UUID.fromString(authentication.getName());
            if (!document.getUploadedBy().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access denied to this document"));
            }
            
            DocumentProcessingResponse response = mapToResponse(document);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Document not found"));
                    
        } catch (Exception e) {
            log.error("Error retrieving document status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve document status: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    @Operation(summary = "Get user's documents", 
               description = "Retrieve all documents uploaded by the authenticated user")
    public ResponseEntity<ApiResponse<List<DocumentProcessingResponse>>> getUserDocuments(
            @Parameter(description = "File type filter") @RequestParam(required = false) String fileType,
            @Parameter(description = "Processing status filter") @RequestParam(required = false) String processingStatus,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            List<Document> documents = documentProcessingService.getUserDocuments(
                    userId, fileType, processingStatus, page, size
            );
            
            List<DocumentProcessingResponse> responses = documents.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses));
            
        } catch (Exception e) {
            log.error("Error retrieving user documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve documents: " + e.getMessage()));
        }
    }

    @PostMapping("/{documentId}/reprocess")
    @Operation(summary = "Reprocess document", 
               description = "Retry processing for failed or incomplete documents")
    public ResponseEntity<ApiResponse<DocumentProcessingResponse>> reprocessDocument(
            @Parameter(description = "Document ID") @PathVariable String documentId,
            Authentication authentication) {
        
        try {
            UUID docId = UUID.fromString(documentId);
            Document document = documentProcessingService.reprocessDocument(docId);
            
            DocumentProcessingResponse response = mapToResponse(document);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Document reprocessing started"));
            
        } catch (Exception e) {
            log.error("Error reprocessing document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reprocess document: " + e.getMessage()));
        }
    }

    @PostMapping("/{documentId}/tags")
    @Operation(summary = "Add tags to document", 
               description = "Add user-defined tags to a document")
    public ResponseEntity<ApiResponse<DocumentProcessingResponse>> addTagsToDocument(
            @Parameter(description = "Document ID") @PathVariable String documentId,
            @Parameter(description = "Tags to add") @RequestBody List<String> tags,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID docId = UUID.fromString(documentId);
            
            Document document = documentProcessingService.addUserTags(docId, userId, tags);
            DocumentProcessingResponse response = mapToResponse(document);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Tags added successfully"));
            
        } catch (Exception e) {
            log.error("Error adding tags to document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add tags: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents", 
               description = "Search documents by content and tags")
    public ResponseEntity<ApiResponse<List<DocumentProcessingResponse>>> searchDocuments(
            @Parameter(description = "Search text") @RequestParam String searchText,
            @Parameter(description = "Domain tags filter") @RequestParam(required = false) List<String> domainTags,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            List<Document> documents = documentProcessingService.searchDocuments(
                    searchText, domainTags, userId
            );
            
            List<DocumentProcessingResponse> responses = documents.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses));
            
        } catch (Exception e) {
            log.error("Error searching documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to search documents: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get document statistics", 
               description = "Get processing statistics for the authenticated user")
    public ResponseEntity<ApiResponse<Object>> getDocumentStatistics(Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            Object statistics = documentProcessingService.getDocumentStatistics(userId);
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
            
        } catch (Exception e) {
            log.error("Error retrieving document statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/tags/popular")
    @Operation(summary = "Get popular domain tags", 
               description = "Get most commonly used domain tags for suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getPopularDomainTags(
            @Parameter(description = "Maximum number of tags") @RequestParam(defaultValue = "20") int limit) {
        
        try {
            List<String> tags = documentProcessingService.getPopularDomainTags(limit);
            return ResponseEntity.ok(ApiResponse.success(tags));
            
        } catch (Exception e) {
            log.error("Error retrieving popular tags", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve popular tags: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", 
               description = "Delete document and all related data (owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Parameter(description = "Document ID") @PathVariable String documentId,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID docId = UUID.fromString(documentId);
            
            documentProcessingService.deleteDocument(docId, userId);
            
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
            
        } catch (Exception e) {
            log.error("Error deleting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete document: " + e.getMessage()));
        }
    }

    /**
     * Map entity to response DTO
     */
    private DocumentProcessingResponse mapToResponse(Document document) {
        return DocumentProcessingResponse.builder()
                .documentId(document.getId().toString())
                .originalFilename(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .processingStatus(document.getProcessingStatus())
                .progressPercentage(document.getProgressPercentage())
                .errorMessage(document.getErrorMessage())
                .uploadedAt(document.getCreatedAt())
                .startedAt(document.getStartedAt())
                .completedAt(document.getCompletedAt())
                .totalPages(document.getTotalPages())
                .totalChunks(document.getTotalChunks())
                .domainTags(document.getDomainTags())
                .userTags(document.getUserTags())
                .qualityScore(document.getQualityScore())
                .languageCode(document.getLanguageCode())
                .contentHash(document.getContentHash())
                .processingTimeMs(document.getCompletedAt() != null && document.getStartedAt() != null ?
                        java.time.Duration.between(document.getStartedAt(), document.getCompletedAt()).toMillis() : null)
                .extractedTextPreview(document.getExtractedText() != null && document.getExtractedText().length() > 200 ?
                        document.getExtractedText().substring(0, 200) + "..." : document.getExtractedText())
                .build();
    }
}
