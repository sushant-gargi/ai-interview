package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.config.InterviewConstants;
import com.aiinterview.ai_interview.dto.session.SessionRequest;
import com.aiinterview.ai_interview.dto.session.SessionResponse;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Resume;
import com.aiinterview.ai_interview.entity.Role;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.ReportRepository;
import com.aiinterview.ai_interview.repository.ResumeRepository;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ChatService;
import com.aiinterview.ai_interview.service.InterviewSessionService;
import com.aiinterview.ai_interview.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    final ReportService reportService;
    final ReportRepository reportRepository;

    @Override
    public SessionResponse createSession(SessionRequest request) {
        if (request.scheduledEnd().isBefore(request.scheduledStart())) {
            throw new BadRequestException("Scheduled end time must be after start time");
        }

        Instant now = Instant.now();
        Instant graceThreshold = now.minus(Duration.ofMinutes(5));
        if (request.scheduledStart().isBefore(graceThreshold)) {
            throw new BadRequestException("Scheduled start time cannot be in the past beyond the 5-minute grace period");
        }
        if (request.scheduledEnd().isBefore(now)) {
            throw new BadRequestException("Scheduled end time cannot be in the past");
        }

        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Prevent a recruiter from scheduling an interview for themselves
        if (request.candidateEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("Recruiter cannot create an interview session for themselves.");
        }

        Instant oneYearAgo = now.minus(Duration.ofDays(365));
        if (sessionRepository.hasCompletedSessionInLastYear(request.candidateEmail(), SessionStatus.COMPLETED, oneYearAgo)) {
            throw new BadRequestException("Candidate has already completed an interview within the last year.");
        }

        Resume resume = null;

        InterviewSession session = InterviewSession.builder()
                .recruiter(user)
                .candidateEmail(request.candidateEmail().toLowerCase())
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Only CANDIDATEs can start a session — prevents a recruiter whose email
        // happens to match candidateEmail from starting their own interview
        if (user.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Only candidates can start interview sessions");
        }

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session", sessionId.toString()));

        if (!session.getCandidateEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("This session does not belong to you");
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BadRequestException("Session is already completed");
        }

        if (session.getStatus() == SessionStatus.EXPIRED) {
            throw new BadRequestException("Session has expired and can no longer be started");
        }

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Session is already in progress");
        }

        // Rigid Gatekeeping: must have a resume uploaded before starting
        if (session.getResume() == null) {
            throw new BadRequestException(
                    "Resume is required. Please upload your resume in the lobby before starting the interview.");
        }

        Instant now = Instant.now();

        // Time lock check: interview has not started yet
        if (now.isBefore(session.getScheduledStart())) {
            throw new BadRequestException(
                    "Interview has not started yet. Scheduled at: "
                            + session.getScheduledStart());
        }

        // ===== GRACE PERIOD CHECK (5-minute window) =====
        // Candidates must join within 5 minutes of scheduledStart
        Instant gracePeriodEnd = session.getScheduledStart().plus(Duration.ofMinutes(InterviewConstants.GRACE_PERIOD_MINUTES));
        if (now.isAfter(gracePeriodEnd)) {
            // Grace period expired: mark session as EXPIRED and auto-generate no-show report
            session.setStatus(SessionStatus.EXPIRED);
            session.setActualEnd(now);
            sessionRepository.save(session);

            log.warn("Session {} grace period expired. Candidate failed to join within 5 minutes of scheduled start.", sessionId);

            // Trigger no-show report generation
            try {
                reportService.generateNoShowReport(sessionId);
            } catch (Exception e) {
                log.error("Failed to generate no-show report for session {}", sessionId, e);
            }

            throw new BadRequestException(
                    "Interview grace period has expired. You must join within 5 minutes of the scheduled start time.");
        }

        // Time lock check: interview window has ended
        if (now.isAfter(session.getScheduledEnd())) {
            session.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(session);
            throw new BadRequestException("Interview window has expired");
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setActualStart(now);
        session.setLastActiveAt(now);

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
    public List<SessionResponse> getRecruiterSessions() {
        Long userId = authUtil.getCurrentUserId();
        return sessionRepository.findByRecruiterId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SessionResponse> getMySessions() {
        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return sessionRepository.findByCandidateEmailIgnoreCase(user.getEmail())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void recordHeartbeat(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (!session.getCandidateEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("This session does not belong to you");
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Heartbeat can only be recorded for IN_PROGRESS sessions. Current status: " + session.getStatus());
        }

        session.setLastActiveAt(Instant.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (!session.getRecruiter().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own sessions");
        }

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BadRequestException("Cannot delete session. The session has already started or completed.");
        }

        Resume resume = session.getResume();
        if (resume != null) {
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(resume.getFilePath()));
            } catch (java.io.IOException e) {
                log.error("Failed to delete resume file: {}", resume.getFilePath(), e);
            }
            session.setResume(null);
            sessionRepository.saveAndFlush(session);
            resumeRepository.delete(resume);
        }

        // Delete any existing report first to avoid FK constraint violation
        // (e.g., a no-show report written by the scheduler before the recruiter deleted the session)
        reportRepository.findBySessionId(sessionId).ifPresent(reportRepository::delete);

        sessionRepository.delete(session);
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
                session.getCreatedAt(),
                session.getResume() != null
        );
    }
}