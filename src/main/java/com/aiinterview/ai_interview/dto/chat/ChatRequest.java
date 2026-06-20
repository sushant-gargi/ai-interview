package com.aiinterview.ai_interview.dto.chat;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 5000) String message
) {
}