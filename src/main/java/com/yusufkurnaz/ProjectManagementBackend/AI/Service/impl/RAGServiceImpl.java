package com.yusufkurnaz.ProjectManagementBackend.AI.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response.RAGQueryResponse;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;
import com.yusufkurnaz.ProjectManagementBackend.AI.Repository.DocumentChunkRepository;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.RAGService;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.VectorSearchService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.LLMService;
import com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Service.DiagramGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service Implementation
 * 
 * RAG Pipeline:
 * 1. Query Embedding: Kullanıcı sorusunu vektöre çevir
 * 2. Vector Search: Benzer chunk'ları bul (cosine similarity)
 * 3. Context Building: Chunk'ları LLM için optimize et
 * 4. LLM Generation: Context ile birlikte cevap üret
 * 5. Post-processing: Cevabı formatla ve metadata ekle
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RAGServiceImpl implements RAGService {

    private final VectorSearchService vectorSearchService;
    private final LLMService llmService;
    private final DiagramGenerationService diagramGenerationService;
    private final DocumentChunkRepository chunkRepository;

    @Value("${app.rag.default-max-chunks:5}")
    private Integer defaultMaxChunks;

    @Value("${app.rag.default-min-similarity:0.3}")
    private Float defaultMinSimilarity;

    @Value("${app.rag.max-context-tokens:3000}")
    private Integer maxContextTokens;

    @Override
    public RAGQueryResponse queryWithRAG(
            String query,
            UUID userId,
            List<String> domainTags,
            Integer maxChunks,
            Float minSimilarity) {

        long startTime = System.currentTimeMillis();
        
        try {
            log.info("RAG Query started - User: {}, Query: '{}'", userId, query);
            
            // 1. Parametreleri normalize et
            maxChunks = maxChunks != null ? maxChunks : defaultMaxChunks;
            minSimilarity = minSimilarity != null ? minSimilarity : defaultMinSimilarity;
            
            // 2. Vector similarity search
            List<DocumentChunk> relevantChunks = vectorSearchService.findSimilarContent(
                    query, domainTags, minSimilarity, maxChunks * 2 // Fazladan getir, sonra filtrele
            );
            
            if (relevantChunks.isEmpty()) {
                log.warn("No relevant chunks found for query: '{}'", query);
                return RAGQueryResponse.error(query, 
                    "İlgili doküman bulunamadı. Lütfen farklı kelimeler kullanarak tekrar deneyin.");
            }
            
            // 3. Chunk'ları relevance'a göre sırala ve optimize et
            List<DocumentChunk> rankedChunks = rankChunksByRelevance(query, relevantChunks);
            List<DocumentChunk> optimizedChunks = optimizeContextWindow(rankedChunks, maxContextTokens);
            
            // 4. Context oluştur
            String context = buildContext(optimizedChunks);
            
            // 5. LLM'e prompt gönder
            String llmResponse = generateLLMResponse(query, context);
            
            // 6. Response oluştur
            long responseTime = System.currentTimeMillis() - startTime;
            
            RAGQueryResponse.QueryMetadata metadata = RAGQueryResponse.QueryMetadata.builder()
                    .timestamp(LocalDateTime.now())
                    .userId(userId)
                    .totalChunksSearched(relevantChunks.size())
                    .chunksUsedInContext(optimizedChunks.size())
                    .embeddingModel("all-MiniLM-L6-v2")
                    .llmModel("Llama-2-7b-chat")
                    .domainTags(domainTags)
                    .minSimilarityThreshold(minSimilarity)
                    .queryType("simple")
                    .build();
            
            RAGQueryResponse response = RAGQueryResponse.success(
                    query, llmResponse, optimizedChunks, responseTime, metadata
            );
            
            // 7. Önerilen sorular ekle
            response.setSuggestedQuestions(generateSuggestedQuestions(query, optimizedChunks));
            
            log.info("RAG Query completed - Response time: {}ms, Chunks used: {}", 
                    responseTime, optimizedChunks.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("RAG Query failed for user {}: {}", userId, e.getMessage(), e);
            return RAGQueryResponse.error(query, "Sistem hatası: " + e.getMessage());
        }
    }

    @Override
    public RAGQueryResponse queryDocument(
            String query,
            UUID documentId,
            UUID userId,
            Integer maxChunks,
            Float minSimilarity) {
        
        try {
            // Sadece belirtilen dokümana ait chunk'ları al
            List<DocumentChunk> documentChunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
            
            if (documentChunks.isEmpty()) {
                return RAGQueryResponse.error(query, "Belirtilen doküman bulunamadı veya işlenmemiş.");
            }
            
            // Query embedding'i ile similarity hesapla
            float[] queryEmbedding = vectorSearchService.getTextEmbedding(query);
            
            List<DocumentChunk> relevantChunks = documentChunks.stream()
                    .filter(chunk -> {
                        if (chunk.getEmbeddingAsFloatArray().length == 0) return false;
                        float similarity = vectorSearchService.calculateSimilarity(
                                queryEmbedding, chunk.getEmbeddingAsFloatArray()
                        );
                        return similarity >= (minSimilarity != null ? minSimilarity : defaultMinSimilarity);
                    })
                    .sorted((c1, c2) -> {
                        float sim1 = vectorSearchService.calculateSimilarity(queryEmbedding, c1.getEmbeddingAsFloatArray());
                        float sim2 = vectorSearchService.calculateSimilarity(queryEmbedding, c2.getEmbeddingAsFloatArray());
                        return Float.compare(sim2, sim1); // Descending
                    })
                    .limit(maxChunks != null ? maxChunks : defaultMaxChunks)
                    .collect(Collectors.toList());
            
            if (relevantChunks.isEmpty()) {
                return RAGQueryResponse.error(query, 
                    "Bu dokümanda sorunuzla ilgili içerik bulunamadı.");
            }
            
            // Normal RAG pipeline'ı devam ettir
            String context = buildContext(relevantChunks);
            String llmResponse = generateLLMResponse(query, context);
            
            RAGQueryResponse.QueryMetadata metadata = RAGQueryResponse.QueryMetadata.builder()
                    .timestamp(LocalDateTime.now())
                    .userId(userId)
                    .totalChunksSearched(documentChunks.size())
                    .chunksUsedInContext(relevantChunks.size())
                    .queryType("document-specific")
                    .build();
            
            return RAGQueryResponse.success(query, llmResponse, relevantChunks, 0L, metadata);
            
        } catch (Exception e) {
            log.error("Document-specific RAG query failed: {}", e.getMessage(), e);
            return RAGQueryResponse.error(query, "Doküman sorgusu başarısız: " + e.getMessage());
        }
    }

    @Override
    public RAGQueryResponse queryWithDiagram(
            String query,
            UUID userId,
            List<String> domainTags,
            String diagramType) {
        
        try {
            // Önce normal RAG query yap
            RAGQueryResponse ragResponse = queryWithRAG(query, userId, domainTags, null, null);
            
            if (ragResponse.getErrorMessage() != null) {
                return ragResponse;
            }
            
            // Diagram üret
            String diagramCode = diagramGenerationService.generateDiagram(
                    ragResponse.getResponse(), diagramType
            );
            
            if (diagramCode != null && !diagramCode.trim().isEmpty()) {
                RAGQueryResponse.DiagramInfo diagramInfo = RAGQueryResponse.DiagramInfo.builder()
                        .diagramType(diagramType)
                        .plantUmlCode(diagramCode)
                        .description("AI tarafından üretilen " + diagramType + " diyagramı")
                        .build();
                
                ragResponse.setDiagramInfo(diagramInfo);
                ragResponse.getMetadata().setQueryType("diagram");
            }
            
            return ragResponse;
            
        } catch (Exception e) {
            log.error("RAG with diagram failed: {}", e.getMessage(), e);
            return RAGQueryResponse.error(query, "Diagram üretimi başarısız: " + e.getMessage());
        }
    }

    @Override
    public RAGQueryResponse conversationalQuery(
            String query,
            UUID userId,
            List<String> conversationHistory,
            List<String> domainTags) {
        
        try {
            // Conversation history'yi query'ye dahil et
            String enhancedQuery = enhanceQueryWithHistory(query, conversationHistory);
            
            RAGQueryResponse response = queryWithRAG(enhancedQuery, userId, domainTags, null, null);
            
            if (response.getMetadata() != null) {
                response.getMetadata().setQueryType("conversational");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Conversational RAG query failed: {}", e.getMessage(), e);
            return RAGQueryResponse.error(query, "Sohbet sorgusu başarısız: " + e.getMessage());
        }
    }

    @Override
    public float evaluateRAGQuality(String query, String response, List<DocumentChunk> sourceChunks) {
      
        // - Semantic similarity between query and response
        // - Factual consistency with source chunks
        // - Response completeness
        // - Hallucination detection
        
        return 0.8f; // Placeholder
    }

    @Override
    public List<DocumentChunk> optimizeContextWindow(List<DocumentChunk> chunks, int maxTokens) {
        List<DocumentChunk> optimized = new ArrayList<>();
        int currentTokens = 0;
        
        for (DocumentChunk chunk : chunks) {
            int chunkTokens = chunk.getTokenCount() != null ? chunk.getTokenCount() : 
                            estimateTokenCount(chunk.getChunkText());
            
            if (currentTokens + chunkTokens <= maxTokens) {
                optimized.add(chunk);
                currentTokens += chunkTokens;
            } else {
                break;
            }
        }
        
        return optimized;
    }

    @Override
    public List<DocumentChunk> rankChunksByRelevance(String query, List<DocumentChunk> chunks) {
        float[] queryEmbedding = vectorSearchService.getTextEmbedding(query);
        
        return chunks.stream()
                .filter(chunk -> chunk.getEmbeddingAsFloatArray().length > 0)
                .sorted((c1, c2) -> {
                    float sim1 = vectorSearchService.calculateSimilarity(queryEmbedding, c1.getEmbeddingAsFloatArray());
                    float sim2 = vectorSearchService.calculateSimilarity(queryEmbedding, c2.getEmbeddingAsFloatArray());
                    return Float.compare(sim2, sim1); // Descending
                })
                .collect(Collectors.toList());
    }

    // Private helper methods
    
    private String buildContext(List<DocumentChunk> chunks) {
        StringBuilder context = new StringBuilder();
        context.append("İlgili Doküman İçerikleri:\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append(String.format("Kaynak %d", i + 1));
            
            if (chunk.getDocument() != null && chunk.getDocument().getTitle() != null) {
                context.append(String.format(" (%s)", chunk.getDocument().getTitle()));
            }
            
            if (chunk.getPageNumber() != null) {
                context.append(String.format(" - Sayfa %d", chunk.getPageNumber()));
            }
            
            context.append(":\n");
            context.append(chunk.getChunkText());
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    private String generateLLMResponse(String query, String context) {
        String prompt = String.format("""
            Sen bir yapay zeka asistanısın. Aşağıdaki doküman içeriklerini kullanarak kullanıcının sorusunu yanıtla.
            
            KURALLAR:
            1. Sadece verilen kaynaklardaki bilgileri kullan
            2. Kaynaklarda olmayan bilgi ekleme
            3. Türkçe ve anlaşılır bir dilde yanıtla
            4. Kaynak belirtmeye gerek yok, direkt cevap ver
            5. Eğer cevap kaynaklarda yoksa "Bu bilgi kaynaklarda mevcut değil" de
            
            %s
            
            SORU: %s
            
            CEVAP:""", context, query);
        
        try {
            return llmService.generateResponse(prompt);
        } catch (Exception e) {
            log.error("LLM response generation failed: {}", e.getMessage());
            return "Üzgünüm, şu anda cevap üretemiyorum. Lütfen daha sonra tekrar deneyin.";
        }
    }
    
    private List<String> generateSuggestedQuestions(String originalQuery, List<DocumentChunk> chunks) {
        
        return Arrays.asList(
            "Bu konu hakkında daha detaylı bilgi verir misin?",
            "İlgili başka örnekler var mı?",
            "Bu konunun pratik uygulamaları nelerdir?"
        );
    }
    
    private String enhanceQueryWithHistory(String query, List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return query;
        }
        
        StringBuilder enhanced = new StringBuilder();
        enhanced.append("Önceki konuşma bağlamı: ");
        enhanced.append(String.join(" ", conversationHistory.subList(
            Math.max(0, conversationHistory.size() - 3), conversationHistory.size()
        )));
        enhanced.append("\n\nMevcut soru: ").append(query);
        
        return enhanced.toString();
    }
    
    private int estimateTokenCount(String text) {
        // Rough estimation: 1 token ≈ 4 characters for Turkish
        return text.length() / 4;
    }
}
