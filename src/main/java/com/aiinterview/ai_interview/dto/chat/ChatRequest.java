package com.aiinterview.ai_interview.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String message
) {
}