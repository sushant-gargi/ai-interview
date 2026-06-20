package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.chat.ChatEndSessionResponse;
import com.aiinterview.ai_interview.dto.chat.ChatHistoryResponse;
import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;

public interface ChatService {
    void initializeSession(Long sessionId);

    ChatResponse sendMessage(Long sessionId, ChatRequest request);

    /**
     * Retrieve full chat history (excluding SYSTEM messages) for a session.
     */
    ChatHistoryResponse getChatHistory(Long sessionId);

    /**
     * Mark session as COMPLETED and auto-generate the evaluation report.
     */
    ChatEndSessionResponse endSession(Long sessionId);
}
