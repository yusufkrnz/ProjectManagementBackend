package com.yusufkurnaz.ProjectManagementBackend.AI.Service;

import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response.RAGQueryResponse;
import com.yusufkurnaz.ProjectManagementBackend.AI.Entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

/**
 * RAG (Retrieval-Augmented Generation) Service Interface
 * 
 * Bu servis şu akışı takip eder:
 * 1. Client'tan gelen soruyu embedding'e çevir
 * 2. Vector similarity ile ilgili chunk'ları bul
 * 3. Chunk'ları context olarak LLM'e gönder
 * 4. LLM'den gelen cevabı formatla ve döndür
 */
public interface RAGService {

    /**
     * Ana RAG query metodu
     * Client'tan gelen soruya PDF içeriğinden faydalanarak cevap üretir
     * 
     * @param query Kullanıcının sorusu
     * @param userId Kullanıcı ID'si (personalization için)
     * @param domainTags Hangi domain'lerde arama yapılacağı
     * @param maxChunks Maksimum kaç chunk kullanılacağı
     * @param minSimilarity Minimum benzerlik skoru (0.0 - 1.0)
     * @return RAG cevabı
     */
    RAGQueryResponse queryWithRAG(
            String query,
            UUID userId,
            List<String> domainTags,
            Integer maxChunks,
            Float minSimilarity
    );

    /**
     * Belirli bir dokümana özel RAG query
     * Sadece belirtilen döküman içinde arama yapar
     */
    RAGQueryResponse queryDocument(
            String query,
            UUID documentId,
            UUID userId,
            Integer maxChunks,
            Float minSimilarity
    );

    /**
     * Multi-modal RAG - hem text hem de diagram üretimi
     * Eğer soru class diagram vs. istiyorsa PlantUML ile diagram da üretir
     */
    RAGQueryResponse queryWithDiagram(
            String query,
            UUID userId,
            List<String> domainTags,
            String diagramType // "class", "sequence", "component" etc.
    );

    /**
     * Conversational RAG - önceki sohbet geçmişini de dikkate alır
     */
    RAGQueryResponse conversationalQuery(
            String query,
            UUID userId,
            List<String> conversationHistory,
            List<String> domainTags
    );

    /**
     * RAG kalitesini değerlendir
     * Verilen cevabın kaynak chunk'larla ne kadar uyumlu olduğunu kontrol eder
     */
    float evaluateRAGQuality(String query, String response, List<DocumentChunk> sourceChunks);

    /**
     * Context window optimizasyonu
     * LLM'in context limitine göre en önemli chunk'ları seçer
     */
    List<DocumentChunk> optimizeContextWindow(List<DocumentChunk> chunks, int maxTokens);

    /**
     * Chunk'ların query ile relevance skorunu hesapla
     */
    List<DocumentChunk> rankChunksByRelevance(String query, List<DocumentChunk> chunks);
}
