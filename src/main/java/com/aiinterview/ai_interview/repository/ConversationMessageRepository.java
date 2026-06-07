package com.aiinterview.ai_interview.repository;

import com.aiinterview.ai_interview.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}

