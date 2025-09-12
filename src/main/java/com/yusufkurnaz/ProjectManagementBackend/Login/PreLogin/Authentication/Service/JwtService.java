package com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service;

import com.yusufkurnaz.ProjectManagementBackend.Common.Model.User;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.Workspace;
import com.yusufkurnaz.ProjectManagementBackend.Common.Model.SecurityConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Autowired
    private SecurityConfig securityConfig;
    
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000; // 24 hours
    
    /**
     * Access token oluşturur
     * @param user User bilgileri
     * @param workspace Workspace bilgileri
     * @return Access token
     */
    public String generateAccessToken(User user, Workspace workspace) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("workspaceId", workspace.getId().toString());
        claims.put("role", "ADMIN"); // TODO: WorkspaceUser'dan alınacak
        claims.put("permissions", "READ_PROJECTS,WRITE_TASKS"); // TODO: Permission sistemi
        
        return createToken(claims, user.getId().toString(), ACCESS_TOKEN_EXPIRATION);
    }
    
    /**
     * Refresh token oluşturur
     * @param user User bilgileri
     * @return Refresh token
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tokenType", "REFRESH");
        
        return createToken(claims, user.getId().toString(), REFRESH_TOKEN_EXPIRATION);
    }
    
    /**
     * Token oluşturur
     * @param claims Claims
     * @param subject Subject
     * @param expiration Expiration time
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        SecretKey key = getSigningKey();
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Token'dan user ID alır
     * @param token JWT token
     * @return User ID
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Token'dan workspace ID alır
     * @param token JWT token
     * @return Workspace ID
     */
    public String extractWorkspaceId(String token) {
        return extractClaim(token, claims -> claims.get("workspaceId", String.class));
    }
    
    /**
     * Token'dan role alır
     * @param token JWT token
     * @return Role
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    
    /**
     * Token'ın geçerli olup olmadığını kontrol eder
     * @param token JWT token
     * @param userId User ID
     * @return Geçerli mi?
     */
    public boolean validateToken(String token, String userId) {
        try {
            String extractedUserId = extractUserId(token);
            return extractedUserId.equals(userId) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Token'ın süresi dolmuş mu kontrol eder
     * @param token JWT token
     * @return Süresi dolmuş mu?
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Token'dan claim alır
     * @param token JWT token
     * @param claimsResolver Claims resolver
     * @return Claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Token'dan tüm claims'leri alır
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Signing key alır
     * @return Secret key
     */
    private SecretKey getSigningKey() {
        String secret = securityConfig.getJwtSecret();
        if (secret == null || secret.isEmpty()) {
            secret = "mySecretKey123456789012345678901234567890"; // Default secret
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
