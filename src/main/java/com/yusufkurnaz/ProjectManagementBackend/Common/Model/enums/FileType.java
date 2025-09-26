package com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supported file types for processing
 * Follows OCP - Easy to extend with new file types
 */
public enum FileType {
    PDF("application/pdf", Set.of(".pdf"), "PDF Document"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
         Set.of(".docx"), "Word Document"),
    DOC("application/msword", Set.of(".doc"), "Legacy Word Document"),
    TXT("text/plain", Set.of(".txt"), "Plain Text");

    private final String mimeType;
    private final Set<String> extensions;
    private final String displayName;

    FileType(String mimeType, Set<String> extensions, String displayName) {
        this.mimeType = mimeType;
        this.extensions = extensions;
        this.displayName = displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get FileType from filename extension
     */
    public static FileType fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid filename: " + fileName);
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        
        return Arrays.stream(values())
                .filter(type -> type.getExtensions().contains(extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + extension));
    }

    /**
     * Get FileType from MIME type
     */
    public static FileType fromMimeType(String mimeType) {
        return Arrays.stream(values())
                .filter(type -> type.getMimeType().equals(mimeType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported MIME type: " + mimeType));
    }

    /**
     * Get all supported extensions as a comma-separated string
     */
    public static String getSupportedExtensions() {
        return Arrays.stream(values())
                .flatMap(type -> type.getExtensions().stream())
                .collect(Collectors.joining(", "));
    }

    /**
     * Check if file type supports text extraction
     */
    public boolean supportsTextExtraction() {
        return this == PDF || this == DOCX || this == DOC || this == TXT;
    }
}
