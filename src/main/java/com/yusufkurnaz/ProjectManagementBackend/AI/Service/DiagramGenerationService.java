package com.yusufkurnaz.ProjectManagementBackend.AI.Service;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.GeneratedDiagram;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for diagram generation operations
 * Follows DIP - Depend on abstractions, not concretions
 */
public interface DiagramGenerationService {

    /**
     * Generate diagram directly from uploaded PDF
     * Main use case - no vector DB search needed
     */
    GeneratedDiagram generateDiagramFromPDF(
            MultipartFile pdfFile, 
            DiagramType diagramType, 
            UUID userId,
            String customPrompt
    );

    /**
     * Generate diagram from existing document
     */
    GeneratedDiagram generateDiagramFromDocument(
            UUID documentId, 
            DiagramType diagramType, 
            UUID userId,
            String customPrompt
    );

    /**
     * Regenerate diagram with different parameters
     */
    GeneratedDiagram regenerateDiagram(
            UUID existingDiagramId,
            DiagramType newDiagramType,
            String customPrompt
    );

    /**
     * Get user's generated diagrams with pagination
     */
    List<GeneratedDiagram> getUserDiagrams(UUID userId, int page, int size);

    /**
     * Get diagram by ID with access control
     */
    GeneratedDiagram getDiagram(UUID diagramId, UUID requestingUserId);

    /**
     * Rate and provide feedback for a diagram
     */
    GeneratedDiagram rateDiagram(UUID diagramId, UUID userId, Integer rating, String feedback);

    /**
     * Update diagram visibility (public/private)
     */
    GeneratedDiagram updateDiagramVisibility(UUID diagramId, UUID userId, Boolean isPublic);

    /**
     * Add tags to diagram
     */
    GeneratedDiagram addTagsToDiagram(UUID diagramId, UUID userId, List<String> tags);

    /**
     * Delete diagram (soft delete)
     */
    void deleteDiagram(UUID diagramId, UUID userId);

    /**
     * Get public diagrams for inspiration
     */
    List<GeneratedDiagram> getPublicDiagrams(DiagramType diagramType, int page, int size);

    /**
     * Export diagram in different formats
     */
    byte[] exportDiagram(UUID diagramId, String format); // "svg", "png", "plantuml"
}
