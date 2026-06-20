package com.aiinterview.ai_interview.dto.chat;

import java.time.Instant;

public record ChatMessageDto(
        String role,       // USER, ASSISTANT
        String message,
        Instant timestamp
) {}
