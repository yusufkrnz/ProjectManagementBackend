package com.yusufkurnaz.ProjectManagementBackend.Common.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.SecurityConfig;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.EncryptionAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    @Autowired
    private SecurityConfig securityConfig;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 32; // 256 bit
    private static final int IV_LENGTH = 16; // 128 bit

    /**
     * Metni şifreler (AES/CBC/PKCS5Padding)
     * @param plainText Şifrelenecek metin
     * @param key Şifreleme anahtarı
     * @return Base64 encoded şifrelenmiş metin
     */
    public String encrypt(String plainText, String key) {
        try {
            // Key validation
            if (key == null || key.length() != KEY_LENGTH) {
                throw new IllegalArgumentException("Key must be exactly " + KEY_LENGTH + " bytes");
            }
            
            // SecretKey oluştur
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            
            // Cipher oluştur
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // Random IV oluştur (CBC için gerekli)
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Şifreleme moduna ayarla
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            // Şifrele
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            
            // IV + şifrelenmiş veriyi birleştir
            byte[] combined = new byte[IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH, encryptedBytes.length);
            
            // Base64'e çevir
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Şifrelenmiş metni çözer
     * @param encryptedText Şifrelenmiş metin
     * @param key Şifreleme anahtarı
     * @return Çözülmüş metin
     */
    public String decrypt(String encryptedText, String key) {
        try {
            // Key validation
            if (key == null || key.length() != KEY_LENGTH) {
                throw new IllegalArgumentException("Key must be exactly " + KEY_LENGTH + " bytes");
            }
            
            // Base64'ten çöz
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            
            // IV'yi ayır
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            
            // Şifrelenmiş veriyi ayır
            byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);
            
            // SecretKey oluştur
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            
            // Cipher oluştur
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // IV'yi ayarla
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Çözme moduna ayarla
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            // Çöz
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Güvenli key oluşturur
     * @return 32 byte random key
     */
    public String generateKey() {
        byte[] key = new byte[KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return new String(key);
    }

    /**
     * Key uzunluğunu kontrol eder
     * @param key Kontrol edilecek key
     * @return Geçerli mi?
     */
    public boolean isValidKey(String key) {
        return key != null && key.length() == KEY_LENGTH;
    }
}
