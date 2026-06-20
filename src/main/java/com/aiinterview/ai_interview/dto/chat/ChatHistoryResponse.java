package com.aiinterview.ai_interview.dto.chat;

import java.util.List;

public record ChatHistoryResponse(
        Long sessionId,
        List<ChatMessageDto> messages
) {}
