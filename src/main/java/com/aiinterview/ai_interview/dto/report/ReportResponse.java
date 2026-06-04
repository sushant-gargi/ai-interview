package com.aiinterview.ai_interview.dto.report;

import com.aiinterview.ai_interview.dto.question.QuestionResponse;

import java.time.Instant;
import java.util.List;

public record ReportResponse(
        Long id,
        Long sessionId,
        String jobRole,
        String summary,
        Integer totalScore,
        Integer maxScore,
        Integer percentage,
        List<QuestionResponse> questions,
        Instant generatedAt
) {
}