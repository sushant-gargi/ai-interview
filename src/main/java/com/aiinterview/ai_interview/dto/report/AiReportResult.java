package com.aiinterview.ai_interview.dto.report;

import com.aiinterview.ai_interview.enums.HiringRecommendation;

import java.util.List;
import java.util.Map;

public record AiReportResult(
        Integer overallScore,
        HiringRecommendation recommendation,
        String summary,
        Map<String, Integer> skillBreakdown,
        List<QuestionScore> questionScores
) {
}
