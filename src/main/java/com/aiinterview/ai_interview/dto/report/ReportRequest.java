package com.aiinterview.ai_interview.dto.report;

public record ReportRequest(
        String summary,
        Integer totalScore,
        Integer maxScore
) {
}