package com.aiinterview.ai_interview.service;

import java.time.Instant;

public interface EmailService {

    /**
     * Sends an interview invite to the candidate when a recruiter creates a session.
     *
     * @param candidateEmail   the candidate's email address
     * @param recruiterName    the name of the recruiter who scheduled the session
     * @param jobRole          the job role being interviewed for
     * @param scheduledStart   the UTC start time of the interview
     * @param scheduledEnd     the UTC end time of the interview
     * @param sessionId        the session ID (for the candidate to reference in the lobby)
     */
    void sendInterviewInvite(String candidateEmail, String recruiterName,
                             String jobRole, Instant scheduledStart, Instant scheduledEnd,
                             Long sessionId);

    /**
     * Sends a report-ready notification to the recruiter when a session report is generated.
     *
     * @param recruiterEmail   the recruiter's email address
     * @param candidateEmail   the candidate's email (for reference)
     * @param jobRole          the job role that was interviewed for
     * @param overallScore     the overall score (0-10)
     * @param recommendation   the hiring recommendation (YES / MAYBE / NO)
     * @param sessionId        the session ID (for the recruiter to fetch the report)
     */
    void sendReportReady(String recruiterEmail, String candidateEmail,
                         String jobRole, int overallScore, String recommendation,
                         Long sessionId);
}
