package com.yusufkurnaz.ProjectManagementBackend.AI.Controller;

import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.request.RAGQueryRequest;
import com.yusufkurnaz.ProjectManagementBackend.AI.Dto.response.RAGQueryResponse;
import com.yusufkurnaz.ProjectManagementBackend.AI.Service.RAGService;
import com.yusufkurnaz.ProjectManagementBackend.Common.Dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * RAG (Retrieval-Augmented Generation) Controller
 * 
 * Bu controller RAG sisteminin ana endpoint'lerini sağlar:
 * - Genel RAG query (tüm dokümanlarda arama)
 * - Dokümana özel RAG query
 * - Diagram ile RAG query
 * - Konuşmalı RAG query
 */
@RestController
@RequestMapping("/api/v1/ai/rag")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RAG Controller", description = "Retrieval-Augmented Generation operations")
public class RAGController {

    private final RAGService ragService;

    /**
     * Ana RAG endpoint
     * Kullanıcının sorusuna tüm dokümanlarda arama yaparak cevap verir
     */
    @PostMapping("/query")
    @Operation(summary = "RAG Query", 
               description = "Ask questions and get AI-powered answers from your documents")
    public ResponseEntity<ApiResponse<RAGQueryResponse>> ragQuery(
            @Valid @RequestBody RAGQueryRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        
        log.info("RAG Query - User: {}, Query: '{}'", userId, request.getQuery());
        
        try {
            RAGQueryResponse response = ragService.queryWithRAG(
                    request.getQuery(),
                    userId,
                    request.getDomainTags(),
                    request.getMaxChunks(),
                    request.getMinSimilarity()
            );
            
            if (response.getErrorMessage() != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getErrorMessage()));
            }
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "RAG query completed successfully")
            );
            
        } catch (Exception e) {
            log.error("RAG Query failed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("RAG query failed: " + e.getMessage()));
        }
    }

    /**
     * Dokümana özel RAG query
     * Sadece belirtilen dokümanda arama yapar
     */
    @PostMapping("/query/document/{documentId}")
    @Operation(summary = "Document-specific RAG Query", 
               description = "Ask questions about a specific document")
    public ResponseEntity<ApiResponse<RAGQueryResponse>> queryDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody RAGQueryRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        
        log.info("Document RAG Query - User: {}, Document: {}, Query: '{}'", 
                userId, documentId, request.getQuery());
        
        try {
            RAGQueryResponse response = ragService.queryDocument(
                    request.getQuery(),
                    documentId,
                    userId,
                    request.getMaxChunks(),
                    request.getMinSimilarity()
            );
            
            if (response.getErrorMessage() != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getErrorMessage()));
            }
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Document RAG query completed successfully")
            );
            
        } catch (Exception e) {
            log.error("Document RAG Query failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Document RAG query failed: " + e.getMessage()));
        }
    }

    /**
     * Diagram ile RAG query
     * Hem text cevabı hem de diagram üretir
     */
    @PostMapping("/query/diagram")
    @Operation(summary = "RAG Query with Diagram", 
               description = "Get answers with automatically generated diagrams")
    public ResponseEntity<ApiResponse<RAGQueryResponse>> queryWithDiagram(
            @Valid @RequestBody RAGQueryRequest request,
            @RequestParam(defaultValue = "class") String diagramType,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        
        log.info("RAG Diagram Query - User: {}, Type: {}, Query: '{}'", 
                userId, diagramType, request.getQuery());
        
        try {
            RAGQueryResponse response = ragService.queryWithDiagram(
                    request.getQuery(),
                    userId,
                    request.getDomainTags(),
                    diagramType
            );
            
            if (response.getErrorMessage() != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getErrorMessage()));
            }
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "RAG diagram query completed successfully")
            );
            
        } catch (Exception e) {
            log.error("RAG Diagram Query failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("RAG diagram query failed: " + e.getMessage()));
        }
    }

    /**
     * Konuşmalı RAG query
     * Önceki sohbet geçmişini dikkate alır
     */
    @PostMapping("/query/conversational")
    @Operation(summary = "Conversational RAG Query", 
               description = "Context-aware conversation with your documents")
    public ResponseEntity<ApiResponse<RAGQueryResponse>> conversationalQuery(
            @Valid @RequestBody RAGQueryRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        
        log.info("Conversational RAG Query - User: {}, Query: '{}'", userId, request.getQuery());
        
        try {
            RAGQueryResponse response = ragService.conversationalQuery(
                    request.getQuery(),
                    userId,
                    request.getConversationHistory(),
                    request.getDomainTags()
            );
            
            if (response.getErrorMessage() != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response.getErrorMessage()));
            }
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Conversational RAG query completed successfully")
            );
            
        } catch (Exception e) {
            log.error("Conversational RAG Query failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Conversational RAG query failed: " + e.getMessage()));
        }
    }

    /**
     * RAG sistem durumu
     */
    @GetMapping("/status")
    @Operation(summary = "RAG System Status", 
               description = "Get current status of RAG system components")
    public ResponseEntity<ApiResponse<Object>> getRAGStatus() {
        
        try {
            //  Implement system status check
            // - HuggingFace API durumu
            // - Vector database durumu
            // - Embedding model durumu
            // - LLM model durumu
            
            return ResponseEntity.ok(
                    ApiResponse.success(
                            java.util.Map.of(
                                    "status", "operational",
                                    "embedding_service", "online",
                                    "llm_service", "online",
                                    "vector_db", "online"
                            ),
                            "RAG system is operational"
                    )
            );
            
        } catch (Exception e) {
            log.error("RAG Status check failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("RAG status check failed: " + e.getMessage()));
        }
    }
}
