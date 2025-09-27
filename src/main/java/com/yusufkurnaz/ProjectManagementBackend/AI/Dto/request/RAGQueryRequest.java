package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG Query Request DTO
 * RAG sorgularında kullanılacak request yapısı
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGQueryRequest {
    
    /**
     * Kullanıcının sorusu (zorunlu)
     */
    @NotBlank(message = "Soru boş olamaz")
    @Size(min = 3, max = 1000, message = "Soru 3-1000 karakter arasında olmalı")
    private String query;
    
    /**
     * Domain etiketleri (opsiyonel)
     * Hangi konularda arama yapılacağını belirtir
     */
    private List<String> domainTags;
    
    /**
     * Maksimum kaç chunk kullanılacağı (opsiyonel)
     * Default: 5
     */
    @Builder.Default
    private Integer maxChunks = 5;
    
    /**
     * Minimum benzerlik skoru (0.0 - 1.0)
     * Default: 0.3
     */
    @Builder.Default
    private Float minSimilarity = 0.3f;
    
    /**
     * Konuşma geçmişi (konuşmalı RAG için)
     */
    private List<String> conversationHistory;
    
    /**
     * Özel parametreler
     */
    @Builder.Default
    private String language = "tr";
    @Builder.Default
    private String responseFormat = "text"; // "text", "json", "markdown"
    @Builder.Default
    private Boolean includeSourceReferences = true;
    @Builder.Default
    private Boolean generateSuggestions = true;
    
    /**
     * Domain etiketlerini temizle ve normalize et
     */
    public List<String> getCleanDomainTags() {
        if (domainTags == null) {
            return null;
        }
        
        return domainTags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .distinct()
                .toList();
    }
    
    /**
     * Parametreleri validate et
     */
    public boolean isValid() {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        if (maxChunks != null && (maxChunks < 1 || maxChunks > 20)) {
            return false;
        }
        
        if (minSimilarity != null && (minSimilarity < 0.0f || minSimilarity > 1.0f)) {
            return false;
        }
        
        return true;
    }
}
