package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.session.SessionRequest;
import com.aiinterview.ai_interview.dto.session.SessionResponse;

import java.util.List;

public interface InterviewSessionService {
    SessionResponse createSession(SessionRequest request);
    SessionResponse startSession(Long sessionId);
    List<SessionResponse> getMySessions();
}