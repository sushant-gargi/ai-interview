package com.aiinterview.ai_interview.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public record JwtUserPrincipal(
        Long userId,
        String email,
        List<GrantedAuthority> authorities
) {
}
