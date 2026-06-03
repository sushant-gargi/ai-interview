package com.aiinterview.ai_interview.service.impl;
import com.aiinterview.ai_interview.dto.report.ReportRequest;
import com.aiinterview.ai_interview.dto.report.ReportResponse;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Report;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.ReportRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    final ReportRepository reportRepository;
    final InterviewSessionRepository sessionRepository;
    final AuthUtil authUtil;

    @Override
    public ReportResponse saveReport(Long sessionId, ReportRequest request) {
        Long userId = authUtil.getCurrentUserId();

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session", sessionId.toString()));

        // Only the owner can save report
        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }

        // Session must be IN_PROGRESS to save report
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Report can only be saved for an in-progress session");
        }

        // Mark session as completed
        session.setStatus(SessionStatus.COMPLETED);
        session.setActualEnd(Instant.now());
        sessionRepository.save(session);

        Report report = Report.builder()
                .session(session)
                .summary(request.summary())
                .totalScore(request.totalScore())
                .maxScore(request.maxScore())
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report saved for session {} with score {}/{}",
                sessionId, saved.getTotalScore(), saved.getMaxScore());

        return toResponse(saved);
    }

    @Override
    public ReportResponse getReport(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session", sessionId.toString()));

        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }

        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report", sessionId.toString()));

        return toResponse(report);
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getSession().getId(),
                report.getSession().getJobRole(),
                report.getSummary(),
                report.getTotalScore(),
                report.getMaxScore(),
                report.getGeneratedAt()
        );
    }
}