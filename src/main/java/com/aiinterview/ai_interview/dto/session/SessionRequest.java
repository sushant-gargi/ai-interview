package com.aiinterview.ai_interview.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record SessionRequest(
        @NotBlank String jobRole,
        @NotBlank String jobDescription,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd
) {
}