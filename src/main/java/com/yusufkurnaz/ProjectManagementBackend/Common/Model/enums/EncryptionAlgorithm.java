package com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums;

public enum EncryptionAlgorithm {
    AES_128("AES", 128),
    AES_192("AES", 192),
    AES_256("AES", 256),
    RSA_2048("RSA", 2048),
    RSA_4096("RSA", 4096);
    
    private final String algorithm;
    private final int keySize;
    
    EncryptionAlgorithm(String algorithm, int keySize) {
        this.algorithm = algorithm;
        this.keySize = keySize;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public int getKeySize() {
        return keySize;
    }
    
    public String getTransformation() {
        if (algorithm.equals("AES")) {
            return "AES/CBC/PKCS5Padding";
        } else if (algorithm.equals("RSA")) {
            return "RSA/ECB/PKCS1Padding";
        }
        throw new UnsupportedOperationException("Unsupported algorithm: " + algorithm);
    }
}
