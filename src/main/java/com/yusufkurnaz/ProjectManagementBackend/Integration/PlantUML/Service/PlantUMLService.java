package com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Service;

import com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Dto.DiagramResult;

/**
 * Service interface for PlantUML diagram generation
 * Follows DIP - Depend on abstractions
 */
public interface PlantUMLService {

    /**
     * Generate diagram from PlantUML code
     */
    DiagramResult generateDiagram(String plantUMLCode, String format);

    /**
     * Generate SVG diagram
     */
    DiagramResult generateSVG(String plantUMLCode);

    /**
     * Generate PNG diagram as byte array
     */
    byte[] generatePNG(String plantUMLCode);

    /**
     * Validate PlantUML code syntax
     */
    boolean isValidPlantUMLCode(String plantUMLCode);

    /**
     * Get supported output formats
     */
    String[] getSupportedFormats();
}
