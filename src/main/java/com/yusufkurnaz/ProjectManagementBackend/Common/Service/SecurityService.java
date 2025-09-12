package com.yusufkurnaz.ProjectManagementBackend.Common.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.SecurityConfig;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.SecurityLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.regex.Pattern;

@Service
public class SecurityService {
    
    @Autowired
    private SecurityConfig securityConfig;
    
    @Autowired
    private HashService hashService;
    
    /**
     * Password güvenlik kontrolü
     * @param password Kontrol edilecek password
     * @return Güvenli mi?
     */
    public boolean isPasswordSecure(String password) {
        if (password == null || password.length() < securityConfig.getMinPasswordLength()) {
            return false;
        }
        
        boolean hasUppercase = securityConfig.isRequireUppercase() ? 
            Pattern.compile("[A-Z]").matcher(password).find() : true;
        
        boolean hasNumbers = securityConfig.isRequireNumbers() ? 
            Pattern.compile("[0-9]").matcher(password).find() : true;
        
        boolean hasSpecialChars = securityConfig.isRequireSpecialChars() ? 
            Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find() : true;
        
        return hasUppercase && hasNumbers && hasSpecialChars;
    }
    
    /**
     * Email format kontrolü
     * @param email Kontrol edilecek email
     * @return Geçerli mi?
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }
    
    /**
     * Güvenli random string oluşturur
     * @param length String uzunluğu
     * @return Random string
     */
    public String generateSecureRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * Güvenli UUID oluşturur
     * @return Random UUID
     */
    public String generateSecureUUID() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * Security level kontrolü
     * @param requiredLevel Gerekli seviye
     * @return Yeterli mi?
     */
    public boolean hasRequiredSecurityLevel(SecurityLevel requiredLevel) {
        SecurityLevel currentLevel = securityConfig.getSecurityLevel();
        return currentLevel.getLevel() >= requiredLevel.getLevel();
    }
    
    /**
     * Input sanitization
     * @param input Temizlenecek input
     * @return Temizlenmiş input
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim()
                .replaceAll("[<>\"'&]", "")
                .replaceAll("\\s+", " ");
    }
    
    /**
     * SQL injection koruması
     * @param input Kontrol edilecek input
     * @return Güvenli mi?
     */
    public boolean isSafeFromSQLInjection(String input) {
        if (input == null) {
            return true;
        }
        
        String[] dangerousPatterns = {
            "'.*--", "'.*;", "'.*\\|\\|", "'.*\\|\\|.*--",
            "'.*UNION.*SELECT", "'.*DROP.*TABLE", "'.*DELETE.*FROM",
            "'.*INSERT.*INTO", "'.*UPDATE.*SET"
        };
        
        String upperInput = input.toUpperCase();
        for (String pattern : dangerousPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(input).find()) {
                return false;
            }
        }
        
        return true;
    }
}
