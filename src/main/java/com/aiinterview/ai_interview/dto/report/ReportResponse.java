package com.aiinterview.ai_interview.dto.report;
import java.time.Instant;

public record ReportResponse(
        Long id,
        Long sessionId,
        String jobRole,
        String summary,
        Integer totalScore,
        Integer maxScore,
        Instant generatedAt
) {
}