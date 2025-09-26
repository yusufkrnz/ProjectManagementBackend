package com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Dto.DiagramResult;
import com.yusufkurnaz.ProjectManagementBackend.Integration.PlantUML.Service.PlantUMLService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of PlantUMLService using PlantUML Java library
 * Follows SRP - Single responsibility: PlantUML operations
 */
@Service
@Slf4j
public class PlantUMLServiceImpl implements PlantUMLService {

    private static final String[] SUPPORTED_FORMATS = {"svg", "png"};

    @Override
    public DiagramResult generateDiagram(String plantUMLCode, String format) {
        return switch (format.toLowerCase()) {
            case "svg" -> generateSVG(plantUMLCode);
            case "png" -> {
                byte[] pngBytes = generatePNG(plantUMLCode);
                yield DiagramResult.successPNG(pngBytes, plantUMLCode, 0L);
            }
            default -> DiagramResult.error("Unsupported format: " + format, plantUMLCode);
        };
    }

    @Override
    public DiagramResult generateSVG(String plantUMLCode) {
        log.debug("Generating SVG diagram from PlantUML code");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate PlantUML code
            if (!isValidPlantUMLCode(plantUMLCode)) {
                return DiagramResult.error("Invalid PlantUML code", plantUMLCode);
            }
            
            SourceStringReader reader = new SourceStringReader(plantUMLCode);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                // Generate SVG
                String description = reader.generateImage(outputStream, new FileFormatOption(FileFormat.SVG));
                
                if (description == null) {
                    return DiagramResult.error("Failed to generate diagram - no output", plantUMLCode);
                }
                
                String svgContent = outputStream.toString(StandardCharsets.UTF_8);
                long generationTime = System.currentTimeMillis() - startTime;
                
                log.debug("SVG diagram generated successfully in {}ms", generationTime);
                
                return DiagramResult.successSVG(svgContent, plantUMLCode, generationTime);
                
            }
        } catch (IOException e) {
            log.error("Error generating SVG diagram", e);
            return DiagramResult.error("IO error during diagram generation: " + e.getMessage(), plantUMLCode);
        } catch (Exception e) {
            log.error("Unexpected error generating SVG diagram", e);
            return DiagramResult.error("Unexpected error: " + e.getMessage(), plantUMLCode);
        }
    }

    @Override
    public byte[] generatePNG(String plantUMLCode) {
        log.debug("Generating PNG diagram from PlantUML code");
        
        try {
            // Validate PlantUML code
            if (!isValidPlantUMLCode(plantUMLCode)) {
                throw new IllegalArgumentException("Invalid PlantUML code");
            }
            
            SourceStringReader reader = new SourceStringReader(plantUMLCode);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                // Generate PNG
                String description = reader.generateImage(outputStream, new FileFormatOption(FileFormat.PNG));
                
                if (description == null) {
                    throw new RuntimeException("Failed to generate PNG diagram - no output");
                }
                
                byte[] pngBytes = outputStream.toByteArray();
                
                log.debug("PNG diagram generated successfully, size: {} bytes", pngBytes.length);
                
                return pngBytes;
                
            }
        } catch (IOException e) {
            log.error("Error generating PNG diagram", e);
            throw new RuntimeException("IO error during PNG generation: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error generating PNG diagram", e);
            throw new RuntimeException("Unexpected error during PNG generation: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isValidPlantUMLCode(String plantUMLCode) {
        if (plantUMLCode == null || plantUMLCode.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = plantUMLCode.trim();
        
        // Check if it starts and ends with proper PlantUML tags
        boolean hasStartTag = trimmed.startsWith("@startuml") || 
                             trimmed.startsWith("@startmindmap") ||
                             trimmed.startsWith("@startgantt");
        
        boolean hasEndTag = trimmed.endsWith("@enduml") || 
                           trimmed.endsWith("@endmindmap") ||
                           trimmed.endsWith("@endgantt");
        
        if (!hasStartTag || !hasEndTag) {
            log.warn("PlantUML code missing proper start/end tags");
            return false;
        }
        
        // Basic syntax validation - check for common issues
        if (trimmed.contains("@startuml") && !trimmed.contains("@enduml")) {
            log.warn("PlantUML code has @startuml but missing @enduml");
            return false;
        }
        
        // Additional validation can be added here
        // For now, we'll do a simple test generation to validate
        try {
            SourceStringReader reader = new SourceStringReader(plantUMLCode);
            try (ByteArrayOutputStream testStream = new ByteArrayOutputStream()) {
                String result = reader.generateImage(testStream, new FileFormatOption(FileFormat.SVG));
                return result != null;
            }
        } catch (Exception e) {
            log.warn("PlantUML code validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String[] getSupportedFormats() {
        return SUPPORTED_FORMATS.clone();
    }

    /**
     * Sanitize PlantUML code to prevent potential security issues
     */
    private String sanitizePlantUMLCode(String plantUMLCode) {
        if (plantUMLCode == null) {
            return null;
        }
        
        // Remove potentially dangerous directives
        String sanitized = plantUMLCode
                .replaceAll("!include\\s+[^\\n]+", "") // Remove include directives
                .replaceAll("!import\\s+[^\\n]+", "") // Remove import directives
                .replaceAll("!procedure\\s+[^\\n]+", "") // Remove procedure definitions
                .trim();
        
        return sanitized;
    }

    /**
     * Add default styling to improve diagram appearance
     */
    private String enhancePlantUMLCode(String plantUMLCode) {
        if (plantUMLCode == null || plantUMLCode.trim().isEmpty()) {
            return plantUMLCode;
        }
        
        // Add default theme and styling if not present
        if (!plantUMLCode.contains("!theme") && !plantUMLCode.contains("skinparam")) {
            StringBuilder enhanced = new StringBuilder();
            
            if (plantUMLCode.trim().startsWith("@startuml")) {
                enhanced.append("@startuml\n");
                enhanced.append("!theme plain\n");
                enhanced.append("skinparam backgroundColor white\n");
                enhanced.append("skinparam classBackgroundColor lightblue\n");
                enhanced.append("skinparam classBorderColor black\n");
                enhanced.append(plantUMLCode.substring(plantUMLCode.indexOf('\n') + 1));
            } else {
                enhanced.append(plantUMLCode);
            }
            
            return enhanced.toString();
        }
        
        return plantUMLCode;
    }
}
