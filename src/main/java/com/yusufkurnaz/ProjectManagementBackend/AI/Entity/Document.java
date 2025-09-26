package com.yusufkurnaz.ProjectManagementBackend.AI.Entity;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.BaseProcessor;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.FileType;
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
 * Document entity for storing uploaded files and their metadata
 * Follows SRP - Single responsibility: Document data representation
 */
@Entity
@Table(name = "ai_documents")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Document extends BaseProcessor {

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "total_chunks")
    @Builder.Default
    private Integer totalChunks = 0;

    // Domain tags for categorization - ⭐ ETIKETLEME SİSTEMİ
    @ElementCollection
    @CollectionTable(name = "document_domain_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> domainTags = new ArrayList<>();

    // User-defined tags - ⭐ KULLANICI ETİKETLEMESİ
    @ElementCollection
    @CollectionTable(name = "document_user_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> userTags = new ArrayList<>();

    @Column(name = "content_hash")
    private String contentHash; // For duplicate detection

    @Column(name = "language_code", length = 5)
    @Builder.Default
    private String languageCode = "tr"; // Default Turkish

    @Column(name = "quality_score")
    private Float qualityScore; // Text extraction quality (0.0 - 1.0)

    // Bidirectional relationship with chunks
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DocumentChunk> chunks = new ArrayList<>();

    // Bidirectional relationship with generated diagrams
    @OneToMany(mappedBy = "sourceDocument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GeneratedDiagram> generatedDiagrams = new ArrayList<>();

    /**
     * Add a domain tag if not already present
     */
    public void addDomainTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !domainTags.contains(tag.toLowerCase())) {
            domainTags.add(tag.toLowerCase());
        }
    }

    /**
     * Add a user tag if not already present
     */
    public void addUserTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !userTags.contains(tag.toLowerCase())) {
            userTags.add(tag.toLowerCase());
        }
    }

    /**
     * Get all tags combined
     */
    public List<String> getAllTags() {
        List<String> allTags = new ArrayList<>(domainTags);
        allTags.addAll(userTags);
        return allTags;
    }

    /**
     * Check if document has specific tag
     */
    public boolean hasTag(String tag) {
        return domainTags.contains(tag.toLowerCase()) || userTags.contains(tag.toLowerCase());
    }

    /**
     * Update total chunks count
     */
    public void updateChunksCount() {
        this.totalChunks = chunks != null ? chunks.size() : 0;
    }
}
