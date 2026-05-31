package com.aiinterview.ai_interview.dto.session;

import com.aiinterview.ai_interview.enums.SessionStatus;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String jobRole,
        String jobDescription,
        SessionStatus status,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant actualStart,
        Instant actualEnd,
        Instant createdAt
) {
}