package com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums;

public enum HashVersion {
    BCrypt_10("BCrypt", 10),
    BCrypt_12("BCrypt", 12),
    BCrypt_14("BCrypt", 14),
    SHA_256("SHA-256", 256),
    SHA_512("SHA-512", 512),
    Argon2("Argon2", 2);
    
    private final String algorithm;
    private final int strength;
    
    HashVersion(String algorithm, int strength) {
        this.algorithm = algorithm;
        this.strength = strength;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public int getStrength() {
        return strength;
    }
    
    public boolean isPasswordHash() {
        return algorithm.equals("BCrypt") || algorithm.equals("Argon2");
    }
    
    public boolean isDataHash() {
        return algorithm.equals("SHA-256") || algorithm.equals("SHA-512");
    }
}
