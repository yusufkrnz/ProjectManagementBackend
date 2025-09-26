package com.yusufkurnaz.ProjectManagementBackend.AI.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Configuration class for file storage settings
 * Follows SRP - Single responsibility: File storage configuration management
 */
@Configuration
@ConfigurationProperties(prefix = "app.file")
@Data
public class FileStorageConfiguration {

    private String uploadDir = "./uploads";
    
    private long maxSize = 52428800; // 50MB
    
    private Set<String> allowedTypes = Set.of("pdf", "docx", "doc", "txt");
    
    private boolean createDirectories = true;
    
    private String tempDir = "./temp";
    
    /**
     * Get upload directory as Path
     */
    public Path getUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
    
    /**
     * Get temp directory as Path
     */
    public Path getTempPath() {
        return Paths.get(tempDir).toAbsolutePath().normalize();
    }
    
    /**
     * Check if file type is allowed
     */
    public boolean isAllowedType(String fileExtension) {
        if (fileExtension == null) return false;
        return allowedTypes.contains(fileExtension.toLowerCase().replace(".", ""));
    }
    
    /**
     * Check if file size is within limits
     */
    public boolean isValidSize(long fileSize) {
        return fileSize > 0 && fileSize <= maxSize;
    }
    
    /**
     * Get human readable max size
     */
    public String getMaxSizeHumanReadable() {
        if (maxSize >= 1024 * 1024) {
            return (maxSize / (1024 * 1024)) + "MB";
        } else if (maxSize >= 1024) {
            return (maxSize / 1024) + "KB";
        } else {
            return maxSize + "B";
        }
    }
}
