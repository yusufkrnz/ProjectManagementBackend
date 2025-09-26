package com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums;

/**
 * Processing status enumeration
 * Follows OCP - Open for extension, closed for modification
 */
public enum ProcessingStatus {
    PENDING("Bekliyor"),
    PROCESSING("İşleniyor"),
    COMPLETED("Tamamlandı"),
    FAILED("Başarısız"),
    CANCELLED("İptal Edildi");

    private final String displayName;

    ProcessingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if status indicates an active processing state
     */
    public boolean isActive() {
        return this == PROCESSING;
    }

    /**
     * Check if status indicates a final state (completed or failed)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * Check if status indicates a successful completion
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
}
