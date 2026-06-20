package com.aiinterview.ai_interview.scheduling;

import com.aiinterview.ai_interview.config.InterviewConstants;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    // Constants are owned by InterviewConstants — no local duplication
    private final InterviewSessionRepository sessionRepository;
    private final ReportService reportService;

    /**
     * Runs every 60 seconds. Enforces the No-Show Rule (Recruit41 Blueprint):
     * Any SCHEDULED session whose grace period has expired is permanently locked
     * as EXPIRED and a no-show report is generated.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void cleanupExpiredGracePeriods() {
        try {
            Instant graceCutoff = Instant.now()
                    .minus(Duration.ofMinutes(InterviewConstants.GRACE_PERIOD_MINUTES));

            List<InterviewSession> expiredSessions = sessionRepository
                    .findTop100ByStatusAndScheduledStartBefore(SessionStatus.SCHEDULED, graceCutoff);

            if (expiredSessions.isEmpty()) {
                log.debug("No expired grace-period sessions found.");
                return;
            }

            log.info("Found {} session(s) with expired grace periods. Processing...", expiredSessions.size());

            for (InterviewSession session : expiredSessions) {
                try {
                    // Mark as EXPIRED first so the report service idempotency check sees
                    // the correct status, and the service remains the single authority on
                    // whether a report already exists.
                    session.setStatus(SessionStatus.EXPIRED);
                    session.setActualEnd(Instant.now());
                    sessionRepository.save(session);

                    // Idempotency is enforced inside generateNoShowReport() — no pre-check needed here
                    reportService.generateNoShowReport(session.getId());

                    log.info("Session {} auto-expired (No-Show Rule). No-show report generated.", session.getId());
                } catch (Exception e) {
                    log.error("Failed to process expired session {}. Will retry on next cycle.", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in cleanupExpiredGracePeriods scheduler", e);
        }
    }

    /**
     * Runs every 60 seconds. Enforces the Disconnect Safety Rule (Recruit41 Blueprint):
     * Any IN_PROGRESS session that exceeds its scheduledEnd + buffer is auto-closed
     * as COMPLETED and an evaluation report is compiled from the transcript.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void cleanupAbandonedSessions() {
        try {
            Instant abandonCutoff = Instant.now()
                    .minus(Duration.ofMinutes(InterviewConstants.ABANDON_BUFFER_MINUTES));

            List<InterviewSession> abandonedSessions = sessionRepository
                    .findTop100ByStatusAndScheduledEndBefore(SessionStatus.IN_PROGRESS, abandonCutoff);

            if (abandonedSessions.isEmpty()) {
                log.debug("No abandoned IN_PROGRESS sessions found.");
                return;
            }

            log.info("Found {} abandoned session(s). Processing...", abandonedSessions.size());

            for (InterviewSession session : abandonedSessions) {
                try {
                    session.setStatus(SessionStatus.COMPLETED);
                    session.setActualEnd(Instant.now());
                    sessionRepository.save(session);

                    // Idempotency is enforced inside generateAbandonedSessionReport()
                    reportService.generateAbandonedSessionReport(session.getId());

                    log.info("Session {} auto-closed (Disconnect Safety Rule). Report generated.", session.getId());
                } catch (Exception e) {
                    log.error("Failed to process abandoned session {}. Will retry on next cycle.", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in cleanupAbandonedSessions scheduler", e);
        }
    }
}
