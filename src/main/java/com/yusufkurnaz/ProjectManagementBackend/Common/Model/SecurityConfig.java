package com.yusufkurnaz.ProjectManagementBackend.Common.Model;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.EncryptionAlgorithm;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.HashVersion;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.SecurityLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityConfig {
    
    // Encryption settings
    private EncryptionAlgorithm defaultEncryption = EncryptionAlgorithm.AES_256;
    private String encryptionKey;
    private int encryptionKeyLength = 32;
    
    // Hash settings
    private HashVersion defaultHash = HashVersion.BCrypt_12;
    private int hashRounds = 12;
    
    // Security level
    private SecurityLevel securityLevel = SecurityLevel.HIGH;
    
    // JWT settings
    private String jwtSecret;
    private long jwtExpiration = 3600000; // 1 hour
    private long refreshExpiration = 86400000; // 24 hours
    
    // Password policy
    private int minPasswordLength = 8;
    private boolean requireSpecialChars = true;
    private boolean requireNumbers = true;
    private boolean requireUppercase = true;
    
    // Session settings
    private int maxLoginAttempts = 5;
    private long lockoutDuration = 300000; // 5 minutes
    
    // Getters and Setters
    public EncryptionAlgorithm getDefaultEncryption() {
        return defaultEncryption;
    }
    
    public void setDefaultEncryption(EncryptionAlgorithm defaultEncryption) {
        this.defaultEncryption = defaultEncryption;
    }
    
    public String getEncryptionKey() {
        return encryptionKey;
    }
    
    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public int getEncryptionKeyLength() {
        return encryptionKeyLength;
    }
    
    public void setEncryptionKeyLength(int encryptionKeyLength) {
        this.encryptionKeyLength = encryptionKeyLength;
    }
    
    public HashVersion getDefaultHash() {
        return defaultHash;
    }
    
    public void setDefaultHash(HashVersion defaultHash) {
        this.defaultHash = defaultHash;
    }
    
    public int getHashRounds() {
        return hashRounds;
    }
    
    public void setHashRounds(int hashRounds) {
        this.hashRounds = hashRounds;
    }
    
    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }
    
    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }
    
    public String getJwtSecret() {
        return jwtSecret;
    }
    
    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
    
    public long getJwtExpiration() {
        return jwtExpiration;
    }
    
    public void setJwtExpiration(long jwtExpiration) {
        this.jwtExpiration = jwtExpiration;
    }
    
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
    
    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
    
    public int getMinPasswordLength() {
        return minPasswordLength;
    }
    
    public void setMinPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }
    
    public boolean isRequireSpecialChars() {
        return requireSpecialChars;
    }
    
    public void setRequireSpecialChars(boolean requireSpecialChars) {
        this.requireSpecialChars = requireSpecialChars;
    }
    
    public boolean isRequireNumbers() {
        return requireNumbers;
    }
    
    public void setRequireNumbers(boolean requireNumbers) {
        this.requireNumbers = requireNumbers;
    }
    
    public boolean isRequireUppercase() {
        return requireUppercase;
    }
    
    public void setRequireUppercase(boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }
    
    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }
    
    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }
    
    public long getLockoutDuration() {
        return lockoutDuration;
    }
    
    public void setLockoutDuration(long lockoutDuration) {
        this.lockoutDuration = lockoutDuration;
    }
}