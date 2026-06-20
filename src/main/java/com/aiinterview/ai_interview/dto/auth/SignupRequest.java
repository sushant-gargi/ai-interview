package com.aiinterview.ai_interview.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @NotBlank @Size(max = 255) String email,
        @Size(min = 4, max = 255) String password,
        @Size(max = 20) String role
) {
}