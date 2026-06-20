package com.aiinterview.ai_interview.dto.chat;

public record ChatEndSessionResponse(
        Long sessionId,
        String status,  // COMPLETED
        String message
) {}
