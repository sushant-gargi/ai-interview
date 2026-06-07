package com.aiinterview.ai_interview.service.impl;
import com.aiinterview.ai_interview.dto.question.QuestionResponse;
import com.aiinterview.ai_interview.dto.report.ReportRequest;
import com.aiinterview.ai_interview.dto.report.ReportResponse;
import com.aiinterview.ai_interview.entity.InterviewQuestion;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Report;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.InterviewQuestionRepository;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.ReportRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ReportService;
import com.aiinterview.ai_interview.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    final ReportRepository reportRepository;
    final InterviewSessionRepository sessionRepository;
    final InterviewQuestionRepository questionRepository;
    final AuthUtil authUtil;
    final ChatService chatService;

    @Override
    public ReportResponse saveReport(Long sessionId, ReportRequest request) {
        Long userId = authUtil.getCurrentUserId();

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session", sessionId.toString()));

        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Report can only be saved for an in-progress session");
        }

        // Calculate score from actual question rows
        List<InterviewQuestion> questions = questionRepository
                .findBySessionIdOrderBySequenceOrderAsc(sessionId);

        int totalScore = questions.stream()
                .filter(q -> q.getScore() != null)
                .mapToInt(InterviewQuestion::getScore)
                .sum();

        // Each question is out of 10
        int maxScore = questions.size() * 10;

        // Mark session completed
        session.setStatus(SessionStatus.COMPLETED);
        session.setActualEnd(Instant.now());
        sessionRepository.save(session);

        String summary;
        if (request.summaryOverride() != null && !request.summaryOverride().isBlank()) {
            summary = request.summaryOverride();
        } else {
            // Attempt to generate AI summary from transcript; fall back to generic placeholder
            try {
                summary = chatService.generateSummaryFromTranscript(sessionId);
            } catch (Exception e) {
                log.error("Failed to generate AI summary for session {}", sessionId, e);
                summary = "Interview completed. AI summary will be generated in the next phase.";
            }
        }

        Report report = Report.builder()
                .session(session)
                .summary(summary)
                .totalScore(totalScore)
                .maxScore(maxScore)
                .build();

        Report saved = reportRepository.save(report);
        log.info("Report saved for session {} — score: {}/{}",
                sessionId, totalScore, maxScore);

        return toResponse(saved, questions);
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

        List<InterviewQuestion> questions = questionRepository
                .findBySessionIdOrderBySequenceOrderAsc(sessionId);

        return toResponse(report, questions);
    }

    private ReportResponse toResponse(Report report, List<InterviewQuestion> questions) {
        int percentage = report.getMaxScore() > 0
                ? (report.getTotalScore() * 100) / report.getMaxScore()
                : 0;

        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getSequenceOrder(),
                        q.getQuestionText(),
                        q.getCandidateAnswer(),
                        q.getAiFeedback(),
                        q.getIdealAnswer(),
                        q.getScore(),
                        q.getAnsweredAt()
                ))
                .toList();

        return new ReportResponse(
                report.getId(),
                report.getSession().getId(),
                report.getSession().getJobRole(),
                report.getSummary(),
                report.getTotalScore(),
                report.getMaxScore(),
                percentage,
                questionResponses,
                report.getGeneratedAt()
        );
    }
}