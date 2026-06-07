package com.aiinterview.ai_interview.dto.chat;
public record ChatResponse(
        Long messageId,
        String assistantMessage,
        Boolean sessionCompleted
) {
}