package com.aiinterview.ai_interview.dto.resume;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        String fileName,
        String parsedText,
        Instant uploadedAt
) {
}
