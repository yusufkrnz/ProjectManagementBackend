package com.yusufkurnaz.ProjectManagementBackend.Common.Config;

import com.yusufkurnaz.ProjectManagementBackend.Login.PreLogin.Authentication.Service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Request'ten Authorization header'ını al
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Authorization header yoksa veya Bearer ile başlamıyorsa, filter chain'i devam ettir
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " kısmını çıkar, sadece token'ı al
        jwt = authHeader.substring(7);
        
        try {
            // Token'dan user email'ini çıkar
            userEmail = jwtService.extractUsername(jwt);
            
            // Eğer email çıkarıldıysa ve SecurityContext'te authentication yoksa
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // UserDetails'i database'den al
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                // Token geçerliyse
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    
                    // Authentication token oluştur
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // Request detaylarını ekle
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // SecurityContext'e authentication'ı set et
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token geçersizse, SecurityContext'i temizle
            SecurityContextHolder.clearContext();
        }
        
        // Filter chain'i devam ettir
        filterChain.doFilter(request, response);
    }
}