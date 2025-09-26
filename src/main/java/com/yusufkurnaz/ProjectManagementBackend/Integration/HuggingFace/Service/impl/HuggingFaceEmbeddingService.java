package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Config.HuggingFaceConfig;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceEmbeddingService implements EmbeddingService {

    private final HuggingFaceConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(config.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] embedText(String text) {
        try {
            String model = config.getEmbeddingModel();
            WebClient.RequestBodySpec spec = client().post()
                    .uri("/pipeline/feature-extraction/" + model);

            JsonNode request = objectMapper.createObjectNode()
                    .putArray("inputs").add(text);

            String response = spec
                    .body(BodyInserters.fromValue(request.toString()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .retry(config.getMaxRetries())
                    .block();

            return parseEmbedding(response);
        } catch (Exception e) {
            log.error("HF embedText error", e);
            throw new RuntimeException("Embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> out = new ArrayList<>();
        for (String t : texts) {
            out.add(embedText(t));
        }
        return out;
    }

    private float[] parseEmbedding(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        // Response can be [[...]] or [[[...]]] depending on model; flatten
        JsonNode arr = root;
        while (arr.isArray() && arr.size() == 1 && arr.get(0).isArray()) {
            arr = arr.get(0);
        }
        float[] vec = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vec[i] = (float) arr.get(i).asDouble();
        }
        return vec;
    }

    @Override
    public String getModelName() {
        return config.getEmbeddingModel();
    }
}
