package com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums;

public enum SecurityLevel {
    LOW("Low Security", 1),
    MEDIUM("Medium Security", 2),
    HIGH("High Security", 3),
    CRITICAL("Critical Security", 4);
    
    private final String description;
    private final int level;
    
    SecurityLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(SecurityLevel other) {
        return this.level > other.level;
    }
    
    public boolean isLowerThan(SecurityLevel other) {
        return this.level < other.level;
    }
    
    public EncryptionAlgorithm getRecommendedEncryption() {
        switch (this) {
            case LOW:
                return EncryptionAlgorithm.AES_128;
            case MEDIUM:
                return EncryptionAlgorithm.AES_192;
            case HIGH:
                return EncryptionAlgorithm.AES_256;
            case CRITICAL:
                return EncryptionAlgorithm.RSA_4096;
            default:
                return EncryptionAlgorithm.AES_256;
        }
    }
    
    public HashVersion getRecommendedHash() {
        switch (this) {
            case LOW:
                return HashVersion.BCrypt_10;
            case MEDIUM:
                return HashVersion.BCrypt_12;
            case HIGH:
                return HashVersion.BCrypt_14;
            case CRITICAL:
                return HashVersion.Argon2;
            default:
                return HashVersion.BCrypt_12;
        }
    }
}
