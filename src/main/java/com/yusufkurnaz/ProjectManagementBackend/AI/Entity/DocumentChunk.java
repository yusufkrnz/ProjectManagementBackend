package com.yusufkurnaz.ProjectManagementBackend.AI.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Document chunk entity with vector embeddings for similarity search
 * Follows SRP - Single responsibility: Text chunk with vector representation
 */
@Entity
@Table(name = "document_chunks")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    // ⭐ VECTOR EMBEDDING - pgvector kullanıyor
    @Column(name = "embedding", columnDefinition = "vector(384)")
    private String embedding; // Hugging Face all-MiniLM-L6-v2 output (384 dimensions)

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(name = "start_position")
    private Integer startPosition; // Character position in original text

    @Column(name = "end_position")
    private Integer endPosition;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "confidence_score")
    private Float confidenceScore; // Embedding quality score (0.0 - 1.0)

    // Domain-specific metadata - ⭐ ETİKETLEME İÇİN
    @Column(name = "content_type")
    private String contentType; // "class_definition", "method_signature", "business_rule", etc.

    @Column(name = "technical_level")
    private String technicalLevel; // "basic", "intermediate", "advanced"

    @Column(name = "language_detected")
    @Builder.Default
    private String languageDetected = "tr";

    /**
     * Convert vector string to float array for similarity calculations
     */
    public float[] getEmbeddingAsFloatArray() {
        if (embedding == null || embedding.trim().isEmpty()) {
            return new float[0];
        }
        
        // Parse PostgreSQL vector format: [0.1, 0.2, 0.3, ...]
        String vectorStr = embedding.replace("[", "").replace("]", "");
        String[] parts = vectorStr.split(",");
        float[] result = new float[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        
        return result;
    }

    /**
     * Set embedding from float array
     */
    public void setEmbeddingFromFloatArray(float[] embeddingArray) {
        if (embeddingArray == null || embeddingArray.length == 0) {
            this.embedding = null;
            return;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embeddingArray.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embeddingArray[i]);
        }
        sb.append("]");
        
        this.embedding = sb.toString();
    }

    /**
     * Check if chunk has valid embedding
     */
    public boolean hasEmbedding() {
        return embedding != null && !embedding.trim().isEmpty();
    }

    /**
     * Get chunk summary for display
     */
    public String getChunkSummary() {
        if (chunkText == null) return "";
        return chunkText.length() > 100 
            ? chunkText.substring(0, 100) + "..." 
            : chunkText;
    }

    /**
     * Check if chunk is from specific page
     */
    public boolean isFromPage(Integer pageNum) {
        return pageNumber != null && pageNumber.equals(pageNum);
    }
}
