package com.yusufkurnaz.ProjectManagementBackend.AI.Controller;

import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request.DiagramGenerationRequest;
import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response.DiagramGenerationResponse;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.GeneratedDiagram;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DiagramGenerationService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Dto.ApiResponse;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for diagram generation operations
 * Follows SRP - Single responsibility: HTTP request handling for diagrams
 */
@RestController
@RequestMapping("/api/v1/ai/diagrams")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Diagram Controller", description = "AI-powered diagram generation operations")
public class DiagramController {

    private final DiagramGenerationService diagramGenerationService;

    @PostMapping(value = "/generate/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate diagram from PDF", 
               description = "Upload PDF file and generate UML diagram using AI")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>> generateDiagramFromPDF(
            @Parameter(description = "PDF file to process") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Diagram generation parameters") @Valid @ModelAttribute DiagramGenerationRequest request,
            Authentication authentication) {
        
        log.info("Generating diagram from PDF: {} for user: {}", file.getOriginalFilename(), authentication.getName());
        
        try {
            UUID userId = UUID.fromString(authentication.getName()); // Assuming JWT contains user ID
            
            GeneratedDiagram diagram = diagramGenerationService.generateDiagramFromPDF(
                    file, 
                    request.getDiagramType(), 
                    userId, 
                    request.getCustomPrompt()
            );
            
            DiagramGenerationResponse response = mapToResponse(diagram);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Diagram generated successfully"));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for diagram generation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error generating diagram from PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate diagram: " + e.getMessage()));
        }
    }

    @PostMapping("/generate/document")
    @Operation(summary = "Generate diagram from existing document", 
               description = "Generate UML diagram from previously uploaded document")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>> generateDiagramFromDocument(
            @Valid @RequestBody DiagramGenerationRequest request,
            Authentication authentication) {
        
        log.info("Generating diagram from document: {} for user: {}", request.getDocumentId(), authentication.getName());
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID documentId = UUID.fromString(request.getDocumentId());
            
            GeneratedDiagram diagram = diagramGenerationService.generateDiagramFromDocument(
                    documentId, 
                    request.getDiagramType(), 
                    userId, 
                    request.getCustomPrompt()
            );
            
            DiagramGenerationResponse response = mapToResponse(diagram);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Diagram generated successfully"));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for diagram generation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
                    
        } catch (SecurityException e) {
            log.warn("Access denied for diagram generation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error generating diagram from document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate diagram: " + e.getMessage()));
        }
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Regenerate existing diagram", 
               description = "Regenerate diagram with different parameters")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>> regenerateDiagram(
            @Valid @RequestBody DiagramGenerationRequest request,
            Authentication authentication) {
        
        log.info("Regenerating diagram: {} for user: {}", request.getExistingDiagramId(), authentication.getName());
        
        try {
            UUID existingDiagramId = UUID.fromString(request.getExistingDiagramId());
            
            GeneratedDiagram diagram = diagramGenerationService.regenerateDiagram(
                    existingDiagramId, 
                    request.getDiagramType(), 
                    request.getCustomPrompt()
            );
            
            DiagramGenerationResponse response = mapToResponse(diagram);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Diagram regenerated successfully"));
            
        } catch (Exception e) {
            log.error("Error regenerating diagram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to regenerate diagram: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    @Operation(summary = "Get user's diagrams", 
               description = "Retrieve all diagrams created by the authenticated user")
    public ResponseEntity<ApiResponse<List<DiagramGenerationResponse>>> getUserDiagrams(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            
            List<GeneratedDiagram> diagrams = diagramGenerationService.getUserDiagrams(userId, page, size);
            List<DiagramGenerationResponse> responses = diagrams.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses));
            
        } catch (Exception e) {
            log.error("Error retrieving user diagrams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve diagrams: " + e.getMessage()));
        }
    }

    @GetMapping("/{diagramId}")
    @Operation(summary = "Get diagram by ID", 
               description = "Retrieve specific diagram with access control")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>> getDiagram(
            @Parameter(description = "Diagram ID") @PathVariable String diagramId,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID diagId = UUID.fromString(diagramId);
            
            GeneratedDiagram diagram = diagramGenerationService.getDiagram(diagId, userId);
            DiagramGenerationResponse response = mapToResponse(diagram);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Diagram not found: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Error retrieving diagram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve diagram: " + e.getMessage()));
        }
    }

    @PostMapping("/{diagramId}/rate")
    @Operation(summary = "Rate diagram", 
               description = "Provide rating and feedback for a diagram")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>> rateDiagram(
            @Parameter(description = "Diagram ID") @PathVariable String diagramId,
            @Parameter(description = "Rating (1-5)") @RequestParam Integer rating,
            @Parameter(description = "Optional feedback") @RequestParam(required = false) String feedback,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID diagId = UUID.fromString(diagramId);
            
            GeneratedDiagram diagram = diagramGenerationService.rateDiagram(diagId, userId, rating, feedback);
            DiagramGenerationResponse response = mapToResponse(diagram);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Rating saved successfully"));
            
        } catch (Exception e) {
            log.error("Error rating diagram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save rating: " + e.getMessage()));
        }
    }

    @PutMapping("/{diagramId}/visibility")
    @Operation(summary = "Update diagram visibility", 
               description = "Make diagram public or private")
    public ResponseEntity<ApiResponse<DiagramGenerationResponse>> updateVisibility(
            @Parameter(description = "Diagram ID") @PathVariable String diagramId,
            @Parameter(description = "Is public") @RequestParam Boolean isPublic,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID diagId = UUID.fromString(diagramId);
            
            GeneratedDiagram diagram = diagramGenerationService.updateDiagramVisibility(diagId, userId, isPublic);
            DiagramGenerationResponse response = mapToResponse(diagram);
            
            return ResponseEntity.ok(ApiResponse.success(response, "Visibility updated successfully"));
            
        } catch (Exception e) {
            log.error("Error updating diagram visibility", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update visibility: " + e.getMessage()));
        }
    }

    @GetMapping("/public")
    @Operation(summary = "Get public diagrams", 
               description = "Retrieve public diagrams for inspiration")
    public ResponseEntity<ApiResponse<List<DiagramGenerationResponse>>> getPublicDiagrams(
            @Parameter(description = "Diagram type filter") @RequestParam(required = false) DiagramType diagramType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        try {
            List<GeneratedDiagram> diagrams = diagramGenerationService.getPublicDiagrams(diagramType, page, size);
            List<DiagramGenerationResponse> responses = diagrams.stream()
                    .map(this::mapToResponse)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses));
            
        } catch (Exception e) {
            log.error("Error retrieving public diagrams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve public diagrams: " + e.getMessage()));
        }
    }

    @GetMapping("/{diagramId}/export/{format}")
    @Operation(summary = "Export diagram", 
               description = "Export diagram in specified format (svg, png, plantuml)")
    public ResponseEntity<byte[]> exportDiagram(
            @Parameter(description = "Diagram ID") @PathVariable String diagramId,
            @Parameter(description = "Export format") @PathVariable String format) {
        
        try {
            UUID diagId = UUID.fromString(diagramId);
            byte[] content = diagramGenerationService.exportDiagram(diagId, format);
            
            String contentType = switch (format.toLowerCase()) {
                case "svg" -> "image/svg+xml";
                case "png" -> "image/png";
                case "plantuml" -> "text/plain";
                default -> "application/octet-stream";
            };
            
            String filename = "diagram_" + diagramId.substring(0, 8) + "." + format.toLowerCase();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(content);
                    
        } catch (Exception e) {
            log.error("Error exporting diagram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{diagramId}")
    @Operation(summary = "Delete diagram", 
               description = "Soft delete diagram (owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteDiagram(
            @Parameter(description = "Diagram ID") @PathVariable String diagramId,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UUID diagId = UUID.fromString(diagramId);
            
            diagramGenerationService.deleteDiagram(diagId, userId);
            
            return ResponseEntity.ok(ApiResponse.success("Diagram deleted successfully"));
            
        } catch (Exception e) {
            log.error("Error deleting diagram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete diagram: " + e.getMessage()));
        }
    }

    /**
     * Map entity to response DTO
     */
    private DiagramGenerationResponse mapToResponse(GeneratedDiagram diagram) {
        return DiagramGenerationResponse.builder()
                .diagramId(diagram.getId().toString())
                .documentId(diagram.getSourceDocument().getId().toString())
                .diagramType(diagram.getDiagramType())
                .title(diagram.getDiagramTitle())
                .description(diagram.getDiagramDescription())
                .svgContent(diagram.getSvgContent())
                .plantUmlCode(diagram.getPlantUmlCode())
                .tags(diagram.getTags())
                .isPublic(diagram.getIsPublic())
                .userRating(diagram.getUserRating())
                .userFeedback(diagram.getUserFeedback())
                .viewCount(diagram.getViewCount())
                .downloadCount(diagram.getDownloadCount())
                .createdAt(diagram.getCreatedAt())
                .generationTimeMs(diagram.getGenerationTimeMs())
                .llmModelUsed(diagram.getLlmModelUsed())
                .qualityScore(diagram.getOverallQualityScore())
                .build();
    }
}
