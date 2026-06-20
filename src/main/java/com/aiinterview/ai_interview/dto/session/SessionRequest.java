package com.aiinterview.ai_interview.dto.session;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SessionRequest(
        @NotBlank @Size(max = 255) String jobRole,
        @NotBlank @Size(max = 10000) String jobDescription,
        @Email @NotBlank String candidateEmail,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd
) {
}