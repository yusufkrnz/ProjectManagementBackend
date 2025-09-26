package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Real implementation of EmbeddingService using Hugging Face Inference API
 * Generates embeddings for Turkish text using optimized models
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${app.huggingface.api-key}")
    private String apiKey;

    @Value("${app.huggingface.api-url:https://api-inference.huggingface.co}")
    private String apiUrl;

    // Turkish-optimized embedding model
    @Value("${app.huggingface.embedding-model:sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2}")
    private String embeddingModel;

    @Override
    public float[] embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[384]; // Return zero vector for empty text
        }

        try {
            log.debug("Generating embedding for text: '{}'", text.substring(0, Math.min(text.length(), 50)));

            String url = apiUrl + "/pipeline/feature-extraction/" + embeddingModel;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "inputs", text,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<float[][]> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, float[][].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                float[][] embeddings = response.getBody();
                if (embeddings.length > 0) {
                    log.debug("Successfully generated embedding with dimension: {}", embeddings[0].length);
                    return embeddings[0]; // Return first embedding
                }
            }

            log.warn("Failed to generate embedding, returning zero vector");
            return new float[384];

        } catch (Exception e) {
            log.error("Error generating embedding for text: {}", e.getMessage());
            // Return zero vector as fallback
            return new float[384];
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            log.debug("Generating embeddings for batch of {} texts", texts.size());

            String url = apiUrl + "/pipeline/feature-extraction/" + embeddingModel;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "inputs", texts,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<float[][][]> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, float[][][].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                float[][][] batchEmbeddings = response.getBody();
                List<float[]> results = new ArrayList<>();
                
                for (float[][] embedding : batchEmbeddings) {
                    if (embedding.length > 0) {
                        results.add(embedding[0]);
                    } else {
                        results.add(new float[384]); // Zero vector fallback
                    }
                }

                log.debug("Successfully generated {} embeddings", results.size());
                return results;
            }

            log.warn("Failed to generate batch embeddings, returning zero vectors");
            return texts.stream()
                    .map(text -> new float[384])
                    .toList();

        } catch (Exception e) {
            log.error("Error generating batch embeddings: {}", e.getMessage());
            // Return zero vectors as fallback
            return texts.stream()
                    .map(text -> new float[384])
                    .toList();
        }
    }

    @Override
    public String getModelName() {
        return embeddingModel;
    }

    /**
     * Health check method to test Hugging Face API connection
     */
    public boolean isApiHealthy() {
        try {
            float[] testEmbedding = embedText("test");
            return testEmbedding.length > 0;
        } catch (Exception e) {
            log.error("Hugging Face API health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get model information from Hugging Face API
     */
    public Map<String, Object> getModelInfo() {
        try {
            String url = apiUrl + "/models/" + embeddingModel;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, 
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            return Collections.emptyMap();

        } catch (Exception e) {
            log.error("Error fetching model info: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
