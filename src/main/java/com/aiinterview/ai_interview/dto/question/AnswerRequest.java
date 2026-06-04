package com.aiinterview.ai_interview.dto.question;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank String answer
) {
}