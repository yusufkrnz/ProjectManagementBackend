package com.yusufkurnaz.ProjectManagementBackend.Common.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.SecurityConfig;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.EncryptionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class KeyManagementService {
    
    @Autowired
    private SecurityConfig securityConfig;
    
    private final Map<String, String> keyCache = new HashMap<>();
    
    /**
     * Workspace için encryption key oluşturur
     * @param workspaceId Workspace ID
     * @return Encryption key
     */
    public String generateWorkspaceKey(UUID workspaceId) {
        String keyId = "workspace_" + workspaceId.toString();
        
        if (keyCache.containsKey(keyId)) {
            return keyCache.get(keyId);
        }
        
        EncryptionAlgorithm algorithm = securityConfig.getDefaultEncryption();
        String key = generateKey(algorithm);
        
        keyCache.put(keyId, key);
        return key;
    }
    
    /**
     * User için encryption key oluşturur
     * @param userId User ID
     * @return Encryption key
     */
    public String generateUserKey(UUID userId) {
        String keyId = "user_" + userId.toString();
        
        if (keyCache.containsKey(keyId)) {
            return keyCache.get(keyId);
        }
        
        EncryptionAlgorithm algorithm = securityConfig.getDefaultEncryption();
        String key = generateKey(algorithm);
        
        keyCache.put(keyId, key);
        return key;
    }
    
    /**
     * Global encryption key oluşturur
     * @return Global encryption key
     */
    public String generateGlobalKey() {
        String keyId = "global";
        
        if (keyCache.containsKey(keyId)) {
            return keyCache.get(keyId);
        }
        
        EncryptionAlgorithm algorithm = securityConfig.getDefaultEncryption();
        String key = generateKey(algorithm);
        
        keyCache.put(keyId, key);
        return key;
    }
    
    /**
     * Key oluşturur
     * @param algorithm Encryption algorithm
     * @return Generated key
     */
    private String generateKey(EncryptionAlgorithm algorithm) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm.getAlgorithm());
            keyGenerator.init(algorithm.getKeySize());
            SecretKey secretKey = keyGenerator.generateKey();
            
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate key for algorithm: " + algorithm.getAlgorithm(), e);
        }
    }
    
    /**
     * Random key oluşturur
     * @param length Key length
     * @return Random key
     */
    public String generateRandomKey(int length) {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[length];
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * Key'i cache'den alır
     * @param keyId Key ID
     * @return Cached key
     */
    public String getCachedKey(String keyId) {
        return keyCache.get(keyId);
    }
    
    /**
     * Key'i cache'e kaydeder
     * @param keyId Key ID
     * @param key Key value
     */
    public void cacheKey(String keyId, String key) {
        keyCache.put(keyId, key);
    }
    
    /**
     * Key'i cache'den siler
     * @param keyId Key ID
     */
    public void removeCachedKey(String keyId) {
        keyCache.remove(keyId);
    }
    
    /**
     * Cache'i temizler
     */
    public void clearCache() {
        keyCache.clear();
    }
    
    /**
     * Key'in geçerli olup olmadığını kontrol eder
     * @param key Key to validate
     * @return Is valid?
     */
    public boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Key uzunluğunu kontrol eder
     * @param key Key to check
     * @param expectedLength Expected length
     * @return Is correct length?
     */
    public boolean isCorrectKeyLength(String key, int expectedLength) {
        if (key == null) {
            return false;
        }
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(key);
            return decodedKey.length == expectedLength;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
