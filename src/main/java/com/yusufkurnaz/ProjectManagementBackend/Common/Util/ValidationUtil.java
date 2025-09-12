package com.yusufkurnaz.ProjectManagementBackend.Common.Util;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.SecurityConfig;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.enums.SecurityLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ValidationUtil {
    
    @Autowired
    private SecurityConfig securityConfig;
    
    // Regex patterns
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String PHONE_PATTERN = "^[+]?[0-9]{10,15}$";
    private static final String NAME_PATTERN = "^[a-zA-ZçğıöşüÇĞIİÖŞÜ\\s]{2,50}$";
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,30}$";
    
    /**
     * Email validation
     * @param email Email to validate
     * @return Is valid?
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        return pattern.matcher(email.toLowerCase().trim()).matches();
    }
    
    /**
     * Password validation
     * @param password Password to validate
     * @return Is valid?
     */
    public boolean isValidPassword(String password) {
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
     * Phone number validation
     * @param phone Phone to validate
     * @return Is valid?
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile(PHONE_PATTERN);
        return pattern.matcher(phone.trim()).matches();
    }
    
    /**
     * Name validation (first name, last name)
     * @param name Name to validate
     * @return Is valid?
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile(NAME_PATTERN);
        return pattern.matcher(name.trim()).matches();
    }
    
    /**
     * Username validation
     * @param username Username to validate
     * @return Is valid?
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        Pattern pattern = Pattern.compile(USERNAME_PATTERN);
        return pattern.matcher(username.trim()).matches();
    }
    
    /**
     * Input sanitization
     * @param input Input to sanitize
     * @return Sanitized input
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim()
                .replaceAll("[<>\"'&]", "")
                .replaceAll("\\s+", " ");
    }
    
    /**
     * SQL injection check
     * @param input Input to check
     * @return Is safe?
     */
    public static boolean isSafeFromSQLInjection(String input) {
        if (input == null) {
            return true;
        }
        
        String[] dangerousPatterns = {
            "'.*--", "'.*;", "'.*\\|\\|", "'.*\\|\\|.*--",
            "'.*UNION.*SELECT", "'.*DROP.*TABLE", "'.*DELETE.*FROM",
            "'.*INSERT.*INTO", "'.*UPDATE.*SET", "'.*ALTER.*TABLE"
        };
        
        for (String pattern : dangerousPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(input).find()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * XSS protection
     * @param input Input to check
     * @return Is safe?
     */
    public static boolean isSafeFromXSS(String input) {
        if (input == null) {
            return true;
        }
        
        String[] dangerousPatterns = {
            "<script.*>", "</script>", "javascript:", "onload=", "onerror=",
            "onclick=", "onmouseover=", "onfocus=", "onblur="
        };
        
        for (String pattern : dangerousPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(input).find()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Length validation
     * @param input Input to validate
     * @param minLength Minimum length
     * @param maxLength Maximum length
     * @return Is valid?
     */
    public static boolean isValidLength(String input, int minLength, int maxLength) {
        if (input == null) {
            return minLength == 0;
        }
        
        int length = input.trim().length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * Not null and not empty validation
     * @param input Input to validate
     * @return Is valid?
     */
    public static boolean isNotNullAndNotEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }
    
    /**
     * Numeric validation
     * @param input Input to validate
     * @return Is numeric?
     */
    public static boolean isNumeric(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        try {
            Double.parseDouble(input.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Integer validation
     * @param input Input to validate
     * @return Is integer?
     */
    public static boolean isInteger(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        try {
            Integer.parseInt(input.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
