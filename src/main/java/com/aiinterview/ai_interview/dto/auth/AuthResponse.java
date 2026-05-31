package com.aiinterview.ai_interview.dto.auth;

public record AuthResponse(
        String token,
        String name,
        String email
) {
}