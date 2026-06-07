package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.session.SessionRequest;
import com.aiinterview.ai_interview.dto.session.SessionResponse;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Resume;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.ResumeRepository;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.InterviewSessionService;
import com.aiinterview.ai_interview.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    final InterviewSessionRepository sessionRepository;
    final ResumeRepository resumeRepository;
    final UserRepository userRepository;
    final AuthUtil authUtil;
    final ChatService chatService;

    @Override
    public SessionResponse createSession(SessionRequest request) {
        if (request.scheduledEnd().isBefore(request.scheduledStart())) {
            throw new BadRequestException("Scheduled end time must be after start time");
        }

        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Must have a resume uploaded before creating session
        Resume resume = resumeRepository
                .findTopByUserIdOrderByUploadedAtDesc(userId)
                .orElseThrow(() -> new BadRequestException(
                        "Please upload your resume before creating a session"));

        InterviewSession session = InterviewSession.builder()
                .user(user)
                .resume(resume)
                .jobRole(request.jobRole())
                .jobDescription(request.jobDescription())
                .scheduledStart(request.scheduledStart())
                .scheduledEnd(request.scheduledEnd())
                .status(SessionStatus.SCHEDULED)
                .build();

        return toResponse(sessionRepository.save(session));
    }

    @Override
    public SessionResponse startSession(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session", sessionId.toString()));

        // Make sure this session belongs to the logged-in user
        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Session is already completed");
        }

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Session is already in progress");
        }

        Instant now = Instant.now();

        // Time lock check
        if (now.isBefore(session.getScheduledStart())) {
            throw new BadRequestException(
                    "Interview has not started yet. Scheduled at: "
                            + session.getScheduledStart());
        }

        if (now.isAfter(session.getScheduledEnd())) {
            session.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(session);
            throw new BadRequestException("Interview window has expired");
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setActualStart(now);

        InterviewSession saved = sessionRepository.save(session);

        // Initialize chat context for the session (system message + assistant greeting)
        try {
            chatService.initializeSession(saved.getId());
        } catch (Exception e) {
            log.error("Failed to initialize chat for session {}", saved.getId(), e);
        }

        return toResponse(saved);
    }

    @Override
    public List<SessionResponse> getMySessions() {
        Long userId = authUtil.getCurrentUserId();
        return sessionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SessionResponse toResponse(InterviewSession session) {
        return new SessionResponse(
                session.getId(),
                session.getJobRole(),
                session.getJobDescription(),
                session.getStatus(),
                session.getScheduledStart(),
                session.getScheduledEnd(),
                session.getActualStart(),
                session.getActualEnd(),
                session.getCreatedAt()
        );
    }
}