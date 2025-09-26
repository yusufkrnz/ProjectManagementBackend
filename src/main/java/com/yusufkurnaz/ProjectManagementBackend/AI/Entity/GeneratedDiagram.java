package com.yusufkurnaz.ProjectManagementBackend.AI.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseEntity;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generated diagram entity for storing PlantUML diagrams and metadata
 * Follows SRP - Single responsibility: Diagram data representation
 */
@Entity
@Table(name = "generated_diagrams")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GeneratedDiagram extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id", nullable = false)
    private Document sourceDocument;

    @Column(name = "generated_by", nullable = false)
    private UUID generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagram_type", nullable = false)
    private DiagramType diagramType;

    @Column(name = "plant_uml_code", columnDefinition = "TEXT", nullable = false)
    private String plantUmlCode;

    @Column(name = "svg_content", columnDefinition = "TEXT")
    private String svgContent;

    @Column(name = "png_file_path")
    private String pngFilePath;

    @Column(name = "diagram_title")
    private String diagramTitle;

    @Column(name = "diagram_description", columnDefinition = "TEXT")
    private String diagramDescription;

    // User feedback and rating system
    @Column(name = "user_rating")
    private Integer userRating; // 1-5 stars

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false; // Can other users see this diagram?

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "download_count")
    @Builder.Default
    private Long downloadCount = 0L;

    // Generation metadata
    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    @Column(name = "llm_model_used")
    private String llmModelUsed;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    // Tags for categorization - ⭐ ETİKETLEME SİSTEMİ
    @ElementCollection
    @CollectionTable(name = "diagram_tags", joinColumns = @JoinColumn(name = "diagram_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // Quality metrics
    @Column(name = "complexity_score")
    private Float complexityScore; // Diagram complexity (0.0 - 1.0)

    @Column(name = "accuracy_score")
    private Float accuracyScore; // How well it represents source document

    /**
     * Add a tag if not already present
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag.toLowerCase())) {
            tags.add(tag.toLowerCase());
        }
    }

    /**
     * Remove a tag
     */
    public void removeTag(String tag) {
        tags.remove(tag.toLowerCase());
    }

    /**
     * Check if diagram has specific tag
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag.toLowerCase());
    }

    /**
     * Increment view count
     */
    public void incrementViewCount() {
        this.viewCount = this.viewCount == null ? 1L : this.viewCount + 1L;
    }

    /**
     * Increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount = this.downloadCount == null ? 1L : this.downloadCount + 1L;
    }

    /**
     * Check if diagram has visual content (SVG or PNG)
     */
    public boolean hasVisualContent() {
        return (svgContent != null && !svgContent.trim().isEmpty()) || 
               (pngFilePath != null && !pngFilePath.trim().isEmpty());
    }

    /**
     * Get diagram file name for downloads
     */
    public String getDownloadFileName() {
        String title = diagramTitle != null ? diagramTitle.replaceAll("[^a-zA-Z0-9]", "_") : "diagram";
        return title + "_" + diagramType.getCode() + "_" + getId().toString().substring(0, 8);
    }

    /**
     * Calculate overall quality score
     */
    public Float getOverallQualityScore() {
        if (accuracyScore == null && complexityScore == null && userRating == null) {
            return null;
        }
        
        float score = 0f;
        int components = 0;
        
        if (accuracyScore != null) {
            score += accuracyScore;
            components++;
        }
        
        if (complexityScore != null) {
            score += complexityScore;
            components++;
        }
        
        if (userRating != null) {
            score += (userRating / 5.0f); // Convert 1-5 rating to 0-1 scale
            components++;
        }
        
        return components > 0 ? score / components : null;
    }
}
