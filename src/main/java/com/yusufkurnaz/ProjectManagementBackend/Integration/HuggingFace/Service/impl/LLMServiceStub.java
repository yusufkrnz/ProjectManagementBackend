package com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.impl;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.DiagramType;
import com.yusufkurnaz.ProjectManagementBackend.Integration.HuggingFace.Service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of LLMService for development/testing
 */
@Service
@org.springframework.context.annotation.Profile("stub")
@Slf4j
public class LLMServiceStub implements LLMService {

    @Override
    public String generateDiagramCode(String prompt, DiagramType diagramType) {
        log.info("Generating {} diagram from prompt (length: {})", diagramType, prompt.length());
        
        // Return a sample PlantUML code based on diagram type
        return switch (diagramType) {
            case CLASS -> generateSampleClassDiagram();
            case SEQUENCE -> generateSampleSequenceDiagram();
            case USECASE -> generateSampleUseCaseDiagram();
            case ACTIVITY -> generateSampleActivityDiagram();
            default -> generateSampleClassDiagram();
        };
    }

    @Override
    public String getModelName() {
        return "stub-model-v1.0";
    }

    @Override
    public boolean isAvailable() {
        return true; // Stub is always available
    }

    private String generateSampleClassDiagram() {
        return """
            @startuml
            !theme plain
            skinparam backgroundColor white
            skinparam classBackgroundColor lightblue
            
            class User {
                - String id
                - String name
                - String email
                + getId()
                + getName()
                + getEmail()
            }
            
            class Document {
                - String id
                - String title
                - String content
                + getId()
                + getTitle()
                + getContent()
            }
            
            class Diagram {
                - String id
                - String type
                - String plantUMLCode
                + getId()
                + getType()
                + generateSVG()
            }
            
            User ||--o{ Document : creates
            Document ||--o{ Diagram : generates
            
            @enduml
            """;
    }

    private String generateSampleSequenceDiagram() {
        return """
            @startuml
            !theme plain
            
            actor User
            participant "Web App" as Web
            participant "AI Service" as AI
            participant "PlantUML" as UML
            
            User -> Web: Upload PDF
            Web -> AI: Process Document
            AI -> AI: Extract Text
            AI -> AI: Generate Prompt
            AI -> UML: Create Diagram
            UML -> AI: Return SVG
            AI -> Web: Diagram Result
            Web -> User: Display Diagram
            
            @enduml
            """;
    }

    private String generateSampleUseCaseDiagram() {
        return """
            @startuml
            !theme plain
            
            left to right direction
            
            actor User
            actor Admin
            
            rectangle "AI Diagram System" {
                User --> (Upload PDF)
                User --> (Generate Diagram)
                User --> (View Diagrams)
                User --> (Export Diagram)
                
                Admin --> (Manage Users)
                Admin --> (View Statistics)
                Admin --> (Configure AI Models)
            }
            
            @enduml
            """;
    }

    private String generateSampleActivityDiagram() {
        return """
            @startuml
            !theme plain
            
            start
            
            :User uploads PDF;
            :Extract text from PDF;
            :Chunk text into segments;
            
            if (Text extraction successful?) then (yes)
                :Generate embeddings;
                :Store in vector database;
                :Create LLM prompt;
                :Generate PlantUML code;
                :Render diagram;
                :Return result to user;
            else (no)
                :Return error message;
            endif
            
            stop
            
            @enduml
            """;
    }
}
