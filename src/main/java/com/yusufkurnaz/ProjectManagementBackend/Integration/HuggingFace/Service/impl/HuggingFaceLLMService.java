package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Config.HuggingFaceConfig;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceLLMService implements LLMService {

    private final HuggingFaceConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(config.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generateDiagramCode(String prompt, DiagramType diagramType) {
        try {
            String model = config.getLlmModel();
            String fullPrompt = buildPrompt(prompt, diagramType);

            JsonNode req = mapper.createObjectNode()
                    .put("inputs", fullPrompt)
                    .set("parameters", mapper.createObjectNode()
                            .put("max_new_tokens", 1024)
                            .put("temperature", 0.2)
                            .put("return_full_text", false)
                    );

            String response = client().post()
                    .uri("/models/" + model)
                    .body(BodyInserters.fromValue(req.toString()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .retry(config.getMaxRetries())
                    .block();

            return extractText(response);
        } catch (Exception e) {
            log.error("HF LLM error", e);
            throw new RuntimeException("LLM generation failed: " + e.getMessage(), e);
        }
    }

    private String extractText(String response) throws Exception {
        JsonNode root = mapper.readTree(response);
        if (root.isArray() && root.size() > 0) {
            JsonNode first = root.get(0);
            if (first.has("generated_text")) {
                return first.get("generated_text").asText();
            }
        }
        return "";
    }

    private String buildPrompt(String userPrompt, DiagramType type) {
        String requirements = "Output valid PlantUML only. Start with " +
                type.getStartTag() + " and end with " + type.getEndTag() + ". " +
                "Use Turkish names if the source is Turkish. Include correct UML relations.";
        return userPrompt + "\n\n" + requirements;
    }

    @Override
    public String getModelName() {
        return config.getLlmModel();
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }
}
