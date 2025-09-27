package com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response;

import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RAG Query Response DTO
 * RAG sisteminden dönen cevap yapısı
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGQueryResponse {
    
    /**
     * Kullanıcının orijinal sorusu
     */
    private String originalQuery;
    
    /**
     * LLM'den gelen cevap
     */
    private String response;
    
    /**
     * Cevabın güven skoru (0.0 - 1.0)
     */
    private Float confidenceScore;
    
    /**
     * Cevap kalite skoru (0.0 - 1.0)
     */
    private Float qualityScore;
    
    /**
     * Cevap üretme süresi (ms)
     */
    private Long responseTimeMs;
    
    /**
     * Kullanılan kaynak chunk'lar
     */
    private List<SourceChunk> sourceChunks;
    
    /**
     * Eğer diagram üretildiyse diagram bilgisi
     */
    private DiagramInfo diagramInfo;
    
    /**
     * Query işleme detayları
     */
    private QueryMetadata metadata;
    
    /**
     * Önerilen follow-up sorular
     */
    private List<String> suggestedQuestions;
    
    /**
     * Hata durumu
     */
    private String errorMessage;
    
    /**
     * Kaynak chunk bilgisi
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceChunk {
        private UUID chunkId;
        private UUID documentId;
        private String documentTitle;
        private String chunkText;
        private Float similarityScore;
        private Integer pageNumber;
        private String sectionTitle;
        private Integer chunkIndex;
        
        /**
         * DocumentChunk entity'den SourceChunk oluştur
         */
        public static SourceChunk fromDocumentChunk(DocumentChunk chunk, Float similarityScore) {
            return SourceChunk.builder()
                    .chunkId(chunk.getId())
                    .documentId(chunk.getDocument().getId())
                    .documentTitle(chunk.getDocument().getTitle())
                    .chunkText(chunk.getChunkText())
                    .similarityScore(similarityScore)
                    .pageNumber(chunk.getPageNumber())
                    .sectionTitle(chunk.getSectionTitle())
                    .chunkIndex(chunk.getChunkIndex())
                    .build();
        }
    }
    
    /**
     * Diagram bilgisi
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramInfo {
        private String diagramType; // "class", "sequence", "component"
        private String plantUmlCode;
        private String diagramUrl; // Generated diagram image URL
        private String description;
    }
    
    /**
     * Query metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryMetadata {
        private LocalDateTime timestamp;
        private UUID userId;
        private Integer totalChunksSearched;
        private Integer chunksUsedInContext;
        private String embeddingModel;
        private String llmModel;
        private List<String> domainTags;
        private Float minSimilarityThreshold;
        private String queryType; // "simple", "conversational", "diagram", "document-specific"
    }
    
    /**
     * Başarılı RAG cevabı oluştur
     */
    public static RAGQueryResponse success(
            String query, 
            String response, 
            List<DocumentChunk> chunks,
            Long responseTime,
            QueryMetadata metadata) {
        
        List<SourceChunk> sourceChunks = chunks.stream()
                .map(chunk -> SourceChunk.fromDocumentChunk(chunk, null))
                .toList();
                
        return RAGQueryResponse.builder()
                .originalQuery(query)
                .response(response)
                .sourceChunks(sourceChunks)
                .responseTimeMs(responseTime)
                .metadata(metadata)
                .confidenceScore(calculateConfidence(response, chunks))
                .qualityScore(calculateQuality(response, chunks))
                .build();
    }
    
    /**
     * Hata durumu için RAG cevabı oluştur
     */
    public static RAGQueryResponse error(String query, String errorMessage) {
        return RAGQueryResponse.builder()
                .originalQuery(query)
                .errorMessage(errorMessage)
                .confidenceScore(0.0f)
                .qualityScore(0.0f)
                .build();
    }
    
    /**
     * Cevabın güven skorunu hesapla
     */
    private static Float calculateConfidence(String response, List<DocumentChunk> chunks) {
        if (response == null || response.trim().isEmpty()) {
            return 0.0f;
        }
        
        // Basit güven skoru hesaplaması
        float baseScore = 0.5f;
        
        // Kaynak chunk sayısına göre artır
        if (chunks != null && !chunks.isEmpty()) {
            baseScore += Math.min(0.3f, chunks.size() * 0.1f);
        }
        
        // Response uzunluğuna göre artır
        if (response.length() > 100) {
            baseScore += 0.2f;
        }
        
        return Math.min(1.0f, baseScore);
    }
    
    /**
     * Cevabın kalite skorunu hesapla
     */
    private static Float calculateQuality(String response, List<DocumentChunk> chunks) {
        if (response == null || response.trim().isEmpty()) {
            return 0.0f;
        }
        
        // Basit kalite skoru hesaplaması
        float qualityScore = 0.6f;
        
        // Kaynak chunk'ların güven skorlarının ortalaması
        if (chunks != null && !chunks.isEmpty()) {
            float avgChunkConfidence = (float) chunks.stream()
                    .filter(c -> c.getConfidenceScore() != null)
                    .mapToDouble(DocumentChunk::getConfidenceScore)
                    .average()
                    .orElse(0.5);
            qualityScore = (qualityScore + avgChunkConfidence) / 2;
        }
        
        return Math.min(1.0f, qualityScore);
    }
}
