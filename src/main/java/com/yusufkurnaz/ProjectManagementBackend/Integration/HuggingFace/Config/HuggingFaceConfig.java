package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.huggingface")
@Data
public class HuggingFaceConfig {
    private String apiUrl;
    private String apiKey;
    private String embeddingModel;
    private String llmModel;
    private int timeout = 60000;
    private int maxRetries = 3;
}
