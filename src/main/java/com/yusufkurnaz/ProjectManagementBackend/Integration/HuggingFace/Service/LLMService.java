package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;

/**
 * Service interface for LLM operations
 * Follows DIP - Depend on abstractions
 */
public interface LLMService {

    /**
     * Generate PlantUML diagram code from text prompt
     */
    String generateDiagramCode(String prompt, DiagramType diagramType);

    /**
     * Get the model name being used
     */
    String getModelName();

    /**
     * Check if service is available
     */
    boolean isAvailable();
}
