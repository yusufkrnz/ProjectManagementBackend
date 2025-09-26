package com.yusufkurnaz.ProjectManagementBackend.Common.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate bean
 * Provides HTTP client for external API calls
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate bean for HTTP requests
     * Used by HuggingFace API integration
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
