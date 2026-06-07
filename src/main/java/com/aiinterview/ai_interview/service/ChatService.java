package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;

public interface ChatService {
    void initializeSession(Long sessionId);

    ChatResponse sendMessage(Long sessionId, ChatRequest request);

    /**
     * Generate a short summary for the session transcript. Returns generated summary text.
     */
    String generateSummaryFromTranscript(Long sessionId);
}

