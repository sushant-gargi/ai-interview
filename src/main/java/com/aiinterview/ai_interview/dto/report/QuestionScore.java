package com.aiinterview.ai_interview.dto.report;

public record QuestionScore(
        String question,
        Integer score,
        String feedback
) {
}
