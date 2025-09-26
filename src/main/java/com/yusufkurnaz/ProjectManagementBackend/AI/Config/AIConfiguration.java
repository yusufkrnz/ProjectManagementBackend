package com.yusufkurnaz.ProjectManagementBackend.AI.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for AI processing settings
 * Follows SRP - Single responsibility: AI configuration management
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai")
@Data
public class AIConfiguration {

    private boolean enabled = true;
    
    private boolean asyncProcessing = true;
    
    private int maxConcurrentJobs = 5;
    
    private int chunkSize = 1000;
    
    private int chunkOverlap = 200;
    
    private int maxRetries = 3;
    
    private long timeoutMs = 60000;
    
    /**
     * Validate configuration on startup
     */
    public void validate() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be non-negative and less than chunk size");
        }
        
        if (maxConcurrentJobs <= 0) {
            throw new IllegalArgumentException("Max concurrent jobs must be positive");
        }
    }
}
