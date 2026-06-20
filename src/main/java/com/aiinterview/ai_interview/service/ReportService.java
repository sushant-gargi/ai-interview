package com.aiinterview.ai_interview.service;

import com.aiinterview.ai_interview.dto.report.ReportResponse;

public interface ReportService {

    /**
     * Retrieve an existing report for a session.
     */
    ReportResponse getReport(Long sessionId);

    /**
     * Auto-generate and persist a report for a normally COMPLETED session.
     * Called automatically by ChatService when the user ends the session.
     * Idempotent — safe to call multiple times.
     */
    void generateCompletedReport(Long sessionId);

    /**
     * Generate a no-show report when a candidate fails to join within the grace period.
     */
    void generateNoShowReport(Long sessionId);

    /**
     * Generate a report for an IN_PROGRESS session that exceeded its scheduled end time.
     */
    void generateAbandonedSessionReport(Long sessionId);
}