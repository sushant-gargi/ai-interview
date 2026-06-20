package com.aiinterview.ai_interview.security;

import com.aiinterview.ai_interview.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

@Component
public class AuthUtil {

    @Value("${app.jwt.secret}")
    String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    long expirationMs;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole() != null ? user.getRole().name() : "CANDIDATE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSecretKey())
                .compact();
    }

    public JwtUserPrincipal verifyAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.get("userId", String.class));
        String email = claims.getSubject();
        String roleStr = claims.get("role", String.class);
        if (roleStr == null) {
            roleStr = "CANDIDATE";
        }
        
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleStr));

        return new JwtUserPrincipal(userId, email, authorities);
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof JwtUserPrincipal userPrincipal)) {
            throw new AuthenticationCredentialsNotFoundException("No JWT found");
        }
        return userPrincipal.userId();
    }

}