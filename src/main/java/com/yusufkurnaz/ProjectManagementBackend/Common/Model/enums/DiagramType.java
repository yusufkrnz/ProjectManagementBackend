package com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums;

/**
 * Supported UML diagram types
 * Follows OCP - Easy to extend with new diagram types
 */
public enum DiagramType {
    CLASS("class", "Sınıf Diyagramı", "@startuml", "@enduml"),
    SEQUENCE("sequence", "Sekans Diyagramı", "@startuml", "@enduml"),
    USECASE("usecase", "Kullanım Durumu Diyagramı", "@startuml", "@enduml"),
    ACTIVITY("activity", "Aktivite Diyagramı", "@startuml", "@enduml"),
    COMPONENT("component", "Bileşen Diyagramı", "@startuml", "@enduml"),
    DEPLOYMENT("deployment", "Dağıtım Diyagramı", "@startuml", "@enduml"),
    STATE("state", "Durum Diyagramı", "@startuml", "@enduml"),
    OBJECT("object", "Nesne Diyagramı", "@startuml", "@enduml");

    private final String code;
    private final String displayName;
    private final String startTag;
    private final String endTag;

    DiagramType(String code, String displayName, String startTag, String endTag) {
        this.code = code;
        this.displayName = displayName;
        this.startTag = startTag;
        this.endTag = endTag;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStartTag() {
        return startTag;
    }

    public String getEndTag() {
        return endTag;
    }

    /**
     * Get DiagramType from code string
     */
    public static DiagramType fromCode(String code) {
        for (DiagramType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown diagram type: " + code);
    }

    /**
     * Wrap PlantUML content with appropriate tags
     */
    public String wrapContent(String content) {
        if (content.trim().startsWith(startTag)) {
            return content; // Already wrapped
        }
        return startTag + "\n" + content + "\n" + endTag;
    }

    /**
     * Extract content from wrapped PlantUML code
     */
    public String extractContent(String wrappedContent) {
        String content = wrappedContent.trim();
        if (content.startsWith(startTag) && content.endsWith(endTag)) {
            return content.substring(startTag.length(), content.length() - endTag.length()).trim();
        }
        return content;
    }

    /**
     * Generate LLM prompt prefix for this diagram type
     */
    public String getLLMPromptPrefix() {
        return switch (this) {
            case CLASS -> "Create a PlantUML class diagram that shows classes, their attributes, methods, and relationships. ";
            case SEQUENCE -> "Create a PlantUML sequence diagram that shows the interaction between objects over time. ";
            case USECASE -> "Create a PlantUML use case diagram that shows actors and their interactions with the system. ";
            case ACTIVITY -> "Create a PlantUML activity diagram that shows the workflow and decision points. ";
            case COMPONENT -> "Create a PlantUML component diagram that shows system components and their dependencies. ";
            case DEPLOYMENT -> "Create a PlantUML deployment diagram that shows the physical deployment of artifacts. ";
            case STATE -> "Create a PlantUML state diagram that shows state transitions of an object. ";
            case OBJECT -> "Create a PlantUML object diagram that shows object instances and their relationships. ";
        };
    }
}
