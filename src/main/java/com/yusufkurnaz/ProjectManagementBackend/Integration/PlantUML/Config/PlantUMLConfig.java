package com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for PlantUML settings
 * Follows SRP - Single responsibility: PlantUML configuration management
 */
@Configuration
@ConfigurationProperties(prefix = "app.plantuml")
@Data
public class PlantUMLConfig {

    private String outputFormat = "svg";
    
    private String theme = "plain";
    
    private long timeout = 30000;
    
    private boolean enableSecurity = true;
    
    private int maxDiagramSize = 1000000; // 1MB
    
    private String[] allowedDirectives = {
        "!theme", "skinparam", "title", "header", "footer"
    };
    
    private String[] blockedDirectives = {
        "!include", "!import", "!procedure", "!function", "!definelong"
    };
    
    /**
     * Check if output format is supported
     */
    public boolean isSupportedFormat(String format) {
        return "svg".equalsIgnoreCase(format) || "png".equalsIgnoreCase(format);
    }
    
    /**
     * Check if directive is allowed
     */
    public boolean isDirectiveAllowed(String directive) {
        if (!enableSecurity) {
            return true;
        }
        
        String lowerDirective = directive.toLowerCase();
        
        // Check blocked directives first
        for (String blocked : blockedDirectives) {
            if (lowerDirective.startsWith(blocked.toLowerCase())) {
                return false;
            }
        }
        
        // Check allowed directives
        for (String allowed : allowedDirectives) {
            if (lowerDirective.startsWith(allowed.toLowerCase())) {
                return true;
            }
        }
        
        // If not explicitly allowed and security is enabled, block it
        return false;
    }
}
