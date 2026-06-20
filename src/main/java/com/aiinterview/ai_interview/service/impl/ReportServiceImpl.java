package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.report.ReportResponse;
import com.aiinterview.ai_interview.entity.ConversationMessage;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Report;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.ConversationMessageRepository;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.ReportRepository;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.EmailService;
import com.aiinterview.ai_interview.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.aiinterview.ai_interview.dto.report.AiReportResult;
import com.aiinterview.ai_interview.enums.HiringRecommendation;
import org.springframework.ai.converter.BeanOutputConverter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    final ReportRepository reportRepository;
    final InterviewSessionRepository sessionRepository;
    final ConversationMessageRepository messageRepository;
    final UserRepository userRepository;
    final AuthUtil authUtil;
    final ChatClient chatClient;
    final EmailService emailService;

    @Override
    public ReportResponse getReport(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        getSessionAndValidateOwner(sessionId, userId);

        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", sessionId.toString()));

        return toResponse(report);
    }

    @Override
    public void generateCompletedReport(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new BadRequestException(
                    "Completed report can only be generated for COMPLETED sessions. Current status: "
                            + session.getStatus());
        }

        // Idempotent — skip if report already exists
        if (reportRepository.findBySessionId(sessionId).isPresent()) {
            log.warn("Report already exists for session {}. Skipping duplicate generation.", sessionId);
            return;
        }

        AiReportResult aiResult = buildStructuredReportFromTranscript(sessionId);

        Report report = Report.builder()
                .session(session)
                .summary(aiResult.summary())
                .overallScore(aiResult.overallScore())
                .recommendation(aiResult.recommendation())
                .skillBreakdown(aiResult.skillBreakdown())
                .questionScores(aiResult.questionScores())
                .build();

        Report saved = reportRepository.save(report);
        log.info("Completed-session report generated for session {} (Report ID: {})", sessionId, saved.getId());

        // Notify recruiter that report is ready
        try {
            emailService.sendReportReady(
                    session.getRecruiter().getEmail(),
                    session.getCandidateEmail(),
                    session.getJobRole(),
                    aiResult.overallScore(),
                    aiResult.recommendation().name(),
                    sessionId
            );
        } catch (Exception e) {
            log.error("Failed to send report-ready email for session {}", sessionId, e);
        }
    }

    @Override
    public void generateNoShowReport(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (session.getStatus() != SessionStatus.EXPIRED) {
            throw new BadRequestException(
                    "No-show report can only be generated for EXPIRED sessions");
        }

        // Idempotent — never write two reports for the same session
        if (reportRepository.findBySessionId(sessionId).isPresent()) {
            log.warn("Report already exists for session {}. Skipping duplicate generation.", sessionId);
            return;
        }

        Report noShowReport = Report.builder()
                .session(session)
                .summary("Candidate failed to join the session within the permitted 5-minute grace window.")
                .overallScore(0)
                .recommendation(HiringRecommendation.NO)
                .skillBreakdown(Map.of())
                .questionScores(List.of())
                .build();

        Report saved = reportRepository.save(noShowReport);
        log.info("No-show report generated for session {} (Report ID: {})", sessionId, saved.getId());

        // Notify recruiter that a no-show report is ready
        try {
            emailService.sendReportReady(
                    session.getRecruiter().getEmail(),
                    session.getCandidateEmail(),
                    session.getJobRole(),
                    0,
                    "NO",
                    sessionId
            );
        } catch (Exception e) {
            log.error("Failed to send no-show report email for session {}", sessionId, e);
        }
    }

    @Override
    public void generateAbandonedSessionReport(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        // Idempotent — never write two reports for the same session
        if (reportRepository.findBySessionId(sessionId).isPresent()) {
            log.warn("Report already exists for session {}. Skipping duplicate generation.", sessionId);
            return;
        }

        AiReportResult aiResult = buildStructuredReportFromTranscript(sessionId);

        Report report = Report.builder()
                .session(session)
                .summary(aiResult.summary())
                .overallScore(aiResult.overallScore())
                .recommendation(aiResult.recommendation())
                .skillBreakdown(aiResult.skillBreakdown())
                .questionScores(aiResult.questionScores())
                .build();

        Report saved = reportRepository.save(report);
        log.info("Abandoned session report generated for session {} (Report ID: {})", sessionId, saved.getId());

        // Notify recruiter that an abandoned-session report is ready
        try {
            emailService.sendReportReady(
                    session.getRecruiter().getEmail(),
                    session.getCandidateEmail(),
                    session.getJobRole(),
                    aiResult.overallScore(),
                    aiResult.recommendation().name(),
                    sessionId
            );
        } catch (Exception e) {
            log.error("Failed to send abandoned report email for session {}", sessionId, e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Fetches a session and verifies the requesting user is its owner.
     */
    private InterviewSession getSessionAndValidateOwner(Long sessionId, Long userId) {
        com.aiinterview.ai_interview.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (!session.getRecruiter().getId().equals(userId) && !session.getCandidateEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("This session does not belong to you");
        }

        return session;
    }

    /**
     * Builds an AI-generated evaluation summary from the session's conversation transcript.
     * Excludes SYSTEM messages (internal prompts) from the text fed to the LLM.
     * Falls back to a descriptive message if the AI call fails.
     */
    private AiReportResult buildStructuredReportFromTranscript(Long sessionId) {
        List<ConversationMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        String transcript = messages.stream()
                .filter(m -> !m.getRole().equalsIgnoreCase("SYSTEM"))
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        if (transcript.isBlank()) {
            return new AiReportResult(
                    0,
                    HiringRecommendation.NO,
                    "Interview session ended. No conversation transcript available to evaluate.",
                    Map.of(),
                    List.of()
            );
        }

        BeanOutputConverter<AiReportResult> converter =
                new BeanOutputConverter<>(AiReportResult.class);

        String summaryPrompt = """
                You are a senior technical talent evaluator. Analyze the following interview dialogue transcript carefully.
                Compile a structured, multi-dimensional, evidence-backed Technical Interview Evaluation Report.
                
                Evaluate the candidate's performance across dimensions such as technical depth, communication, and problem-solving.
                Determine an overall score (1-10) and a hiring recommendation.
                Extract individual questions asked by the assistant and score the candidate's answer to each (1-10) along with brief feedback.
                
                Transcript Content:
                %s
                
                %s
                """.formatted(transcript, converter.getFormat());

        try {
            String resultStr = chatClient.prompt()
                    .user(summaryPrompt)
                    .call()
                    .content();

            if (resultStr == null || resultStr.isBlank()) {
                return new AiReportResult(
                        0, HiringRecommendation.NO, "Summary generation returned an empty response.", Map.of(), List.of());
            }

            // Strip markdown fences that free models (like LLaMA 3.1-8b) frequently add around JSON output
            resultStr = resultStr
                    .replaceAll("(?i)```json", "")
                    .replaceAll("```", "")
                    .trim();

            int startIndex = resultStr.indexOf('{');
            int endIndex = resultStr.lastIndexOf('}');
            if (startIndex >= 0 && endIndex > startIndex) {
                resultStr = resultStr.substring(startIndex, endIndex + 1);
            }

            return converter.convert(resultStr);
        } catch (Exception e) {
            log.error("AI summary generation failed for session {}", sessionId, e);
            return new AiReportResult(
                    0, HiringRecommendation.NO, "Summary generation failed due to an AI service error.", Map.of(), List.of());
        }
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getSession().getId(),
                report.getSession().getJobRole(),
                report.getSummary(),
                report.getOverallScore(),
                report.getRecommendation(),
                report.getSkillBreakdown(),
                report.getQuestionScores(),
                report.getGeneratedAt()
        );
    }
}
