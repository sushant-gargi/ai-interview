package com.aiinterview.ai_interview.dto.question;

import java.time.Instant;

public record QuestionResponse(
        Long id,
        Integer sequenceOrder,
        String questionText,
        String candidateAnswer,
        String aiFeedback,
        String idealAnswer,
        Integer score,
        Instant answeredAt
) {
}