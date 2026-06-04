package com.aiinterview.ai_interview.dto.report;

public record ReportRequest(
        String summaryOverride  // optional — if null AI will generate later
) {
}