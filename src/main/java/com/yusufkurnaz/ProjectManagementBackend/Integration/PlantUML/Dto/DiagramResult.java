package com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for PlantUML diagram generation result
 * Follows SRP - Single responsibility: Data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagramResult {

    private String svgContent;
    
    private byte[] pngContent;
    
    private String format;
    
    private boolean success;
    
    private String errorMessage;
    
    private long generationTimeMs;
    
    private String plantUMLCode;

    /**
     * Create successful result with SVG content
     */
    public static DiagramResult successSVG(String svgContent, String plantUMLCode, long generationTimeMs) {
        return DiagramResult.builder()
                .svgContent(svgContent)
                .format("svg")
                .success(true)
                .plantUMLCode(plantUMLCode)
                .generationTimeMs(generationTimeMs)
                .build();
    }

    /**
     * Create successful result with PNG content
     */
    public static DiagramResult successPNG(byte[] pngContent, String plantUMLCode, long generationTimeMs) {
        return DiagramResult.builder()
                .pngContent(pngContent)
                .format("png")
                .success(true)
                .plantUMLCode(plantUMLCode)
                .generationTimeMs(generationTimeMs)
                .build();
    }

    /**
     * Create error result
     */
    public static DiagramResult error(String errorMessage, String plantUMLCode) {
        return DiagramResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .plantUMLCode(plantUMLCode)
                .build();
    }
}
