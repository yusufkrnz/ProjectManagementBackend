package com.yusufkurnaz.ProjectManagementBackend.Integration.GitHub.Config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * Test configuration for GitHub integration tests
 * Provides mock beans and test-specific configurations
 */
@TestConfiguration
@Profile("test")
public class GitHubTestConfig {

    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        return new RestTemplate();
    }
}






