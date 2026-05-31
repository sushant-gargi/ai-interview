package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.auth.AuthResponse;
import com.aiinterview.ai_interview.dto.auth.LoginRequest;
import com.aiinterview.ai_interview.dto.auth.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}
