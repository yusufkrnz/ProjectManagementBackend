package com.yusufkurnaz.ProjectManagementBackend.AI.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.Document;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.GeneratedDiagram;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.GeneratedDiagramRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DiagramGenerationService;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.DocumentProcessingService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.LLMService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Service.PlantUMLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of DiagramGenerationService
 * Follows SOLID principles with dependency injection
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DiagramGenerationServiceImpl implements DiagramGenerationService {

    private final DocumentProcessingService documentProcessingService;
    private final LLMService llmService;
    private final PlantUMLService plantUMLService;
    private final DocumentRepository documentRepository;
    private final GeneratedDiagramRepository diagramRepository;

    @Override
    public GeneratedDiagram generateDiagramFromPDF(
            MultipartFile pdfFile, 
            DiagramType diagramType, 
            UUID userId,
            String customPrompt) {
        
        log.info("Starting diagram generation from PDF for user: {}, type: {}", userId, diagramType);
        
        try {
            // Step 1: Process document (no vector search needed)
            Document document = documentProcessingService.processDocument(pdfFile, userId, List.of());
            
            // Step 2: Generate diagram from document
            return generateDiagramFromDocument(document.getId(), diagramType, userId, customPrompt);
            
        } catch (Exception e) {
            log.error("Error generating diagram from PDF for user: {}", userId, e);
            throw new RuntimeException("Diagram generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public GeneratedDiagram generateDiagramFromDocument(
            UUID documentId, 
            DiagramType diagramType, 
            UUID userId,
            String customPrompt) {
        
        log.info("Generating diagram from document: {}, type: {}", documentId, diagramType);
        
        // Validate document access
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        if (!document.getUploadedBy().equals(userId)) {
            throw new SecurityException("Access denied to document: " + documentId);
        }
        
        if (!document.isCompleted()) {
            throw new IllegalStateException("Document processing not completed yet");
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Step 1: Create LLM prompt
            String prompt = buildDiagramPrompt(document, diagramType, customPrompt);
            
            // Step 2: Generate PlantUML code using LLM
            String plantUMLCode = llmService.generateDiagramCode(prompt, diagramType);
            
            // Step 3: Generate visual diagram
            var diagramResult = plantUMLService.generateDiagram(plantUMLCode, "svg");
            
            // Step 4: Save generated diagram
            GeneratedDiagram diagram = GeneratedDiagram.builder()
                    .sourceDocument(document)
                    .generatedBy(userId)
                    .diagramType(diagramType)
                    .plantUmlCode(plantUMLCode)
                    .svgContent(diagramResult.getSvgContent())
                    .diagramTitle(generateDiagramTitle(document, diagramType))
                    .generationTimeMs(System.currentTimeMillis() - startTime)
                    .llmModelUsed(llmService.getModelName())
                    .promptUsed(prompt)
                    .isPublic(false)
                    .viewCount(0L)
                    .downloadCount(0L)
                    .build();
            
            // Add automatic tags based on document
            diagram.getTags().addAll(document.getDomainTags());
            diagram.getTags().add(diagramType.getCode());
            
            GeneratedDiagram savedDiagram = diagramRepository.save(diagram);
            
            log.info("Diagram generated successfully: {} in {}ms", savedDiagram.getId(), 
                    savedDiagram.getGenerationTimeMs());
            
            return savedDiagram;
            
        } catch (Exception e) {
            log.error("Error generating diagram from document: {}", documentId, e);
            throw new RuntimeException("Diagram generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public GeneratedDiagram regenerateDiagram(
            UUID existingDiagramId,
            DiagramType newDiagramType,
            String customPrompt) {
        
        GeneratedDiagram existingDiagram = diagramRepository.findById(existingDiagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + existingDiagramId));
        
        return generateDiagramFromDocument(
                existingDiagram.getSourceDocument().getId(),
                newDiagramType,
                existingDiagram.getGeneratedBy(),
                customPrompt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneratedDiagram> getUserDiagrams(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return diagramRepository.findByGeneratedByAndIsActiveTrueOrderByCreatedAtDesc(userId, pageRequest)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedDiagram getDiagram(UUID diagramId, UUID requestingUserId) {
        GeneratedDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + diagramId));
        
        // Check access permissions
        if (!diagram.getGeneratedBy().equals(requestingUserId) && !diagram.getIsPublic()) {
            throw new SecurityException("Access denied to diagram: " + diagramId);
        }
        
        // Increment view count
        diagram.incrementViewCount();
        return diagramRepository.save(diagram);
    }

    @Override
    public GeneratedDiagram rateDiagram(UUID diagramId, UUID userId, Integer rating, String feedback) {
        GeneratedDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + diagramId));
        
        if (!diagram.getGeneratedBy().equals(userId)) {
            throw new SecurityException("Only diagram owner can rate it");
        }
        
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        diagram.setUserRating(rating);
        diagram.setUserFeedback(feedback);
        
        return diagramRepository.save(diagram);
    }

    @Override
    public GeneratedDiagram updateDiagramVisibility(UUID diagramId, UUID userId, Boolean isPublic) {
        GeneratedDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + diagramId));
        
        if (!diagram.getGeneratedBy().equals(userId)) {
            throw new SecurityException("Only diagram owner can change visibility");
        }
        
        diagram.setIsPublic(isPublic);
        return diagramRepository.save(diagram);
    }

    @Override
    public GeneratedDiagram addTagsToDiagram(UUID diagramId, UUID userId, List<String> tags) {
        GeneratedDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + diagramId));
        
        if (!diagram.getGeneratedBy().equals(userId)) {
            throw new SecurityException("Only diagram owner can add tags");
        }
        
        tags.forEach(diagram::addTag);
        return diagramRepository.save(diagram);
    }

    @Override
    public void deleteDiagram(UUID diagramId, UUID userId) {
        GeneratedDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + diagramId));
        
        if (!diagram.getGeneratedBy().equals(userId)) {
            throw new SecurityException("Only diagram owner can delete it");
        }
        
        diagram.setIsActive(false);
        diagramRepository.save(diagram);
        
        log.info("Diagram soft deleted: {} by user: {}", diagramId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneratedDiagram> getPublicDiagrams(DiagramType diagramType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return diagramRepository.findPublicDiagramsByType(diagramType, pageRequest);
    }

    @Override
    public byte[] exportDiagram(UUID diagramId, String format) {
        GeneratedDiagram diagram = diagramRepository.findById(diagramId)
                .orElseThrow(() -> new IllegalArgumentException("Diagram not found: " + diagramId));
        
        // Increment download count
        diagram.incrementDownloadCount();
        diagramRepository.save(diagram);
        
        return switch (format.toLowerCase()) {
            case "svg" -> diagram.getSvgContent().getBytes();
            case "png" -> plantUMLService.generatePNG(diagram.getPlantUmlCode());
            case "plantuml" -> diagram.getPlantUmlCode().getBytes();
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        };
    }

    /**
     * Build LLM prompt for diagram generation
     */
    private String buildDiagramPrompt(Document document, DiagramType diagramType, String customPrompt) {
        StringBuilder prompt = new StringBuilder();
        
        // Add diagram type specific prefix
        prompt.append(diagramType.getLLMPromptPrefix());
        
        // Add custom instructions if provided
        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            prompt.append("\nAdditional instructions: ").append(customPrompt);
        }
        
        // Add document context
        prompt.append("\n\nDocument content to analyze:\n");
        prompt.append(document.getExtractedText());
        
        // Add domain-specific instructions
        if (!document.getDomainTags().isEmpty()) {
            prompt.append("\n\nDomain context: This document is related to: ");
            prompt.append(String.join(", ", document.getDomainTags()));
        }
        
        // Add output format requirements
        prompt.append("\n\nOutput requirements:");
        prompt.append("\n- Generate valid PlantUML code only");
        prompt.append("\n- Start with ").append(diagramType.getStartTag());
        prompt.append("\n- End with ").append(diagramType.getEndTag());
        prompt.append("\n- Use Turkish names if the source content is in Turkish");
        prompt.append("\n- Include proper UML notation and relationships");
        prompt.append("\n- Focus on the most important elements from the document");
        
        return prompt.toString();
    }

    /**
     * Generate appropriate title for the diagram
     */
    private String generateDiagramTitle(Document document, DiagramType diagramType) {
        String baseTitle = document.getOriginalFilename().replaceAll("\\.[^.]+$", ""); // Remove extension
        return baseTitle + " - " + diagramType.getDisplayName();
    }
}
