package com.yusufkurnaz.ProjectManagementBackend.Common.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.SecurityConfig;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.HashVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Service
public class HashService {
    
    @Autowired
    private SecurityConfig securityConfig;
    
    /**
     * Password için BCrypt hash oluşturur
     * @param password Şifrelenecek password
     * @return BCrypt hash
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        HashVersion hashVersion = securityConfig.getDefaultHash();
        int rounds = securityConfig.getHashRounds();
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(rounds);
        return encoder.encode(password);
    }
    
    /**
     * Password doğrulama
     * @param password Düz password
     * @param hash Hash'lenmiş password
     * @return Doğru mu?
     */
    public boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        
        HashVersion hashVersion = securityConfig.getDefaultHash();
        int rounds = securityConfig.getHashRounds();
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(rounds);
        return encoder.matches(password, hash);
    }
    
    /**
     * Email için SHA-256 hash oluşturur (search için)
     * @param email Hash'lenecek email
     * @return SHA-256 hash
     */
    public String hashEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.toLowerCase().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Random salt oluşturur
     * @param length Salt uzunluğu
     * @return Random salt
     */
    public String generateSalt(int length) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }
    
    /**
     * Byte array'i hex string'e çevirir
     * @param bytes Byte array
     * @return Hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
