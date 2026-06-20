package com.aiinterview.ai_interview.config;

/**
 * Single source of truth for all domain-level business constants.
 * All timing and scoring rules enforced by the Recruit41 Blueprint live here.
 * Centralizing these prevents silent drift when values need to change.
 */
public final class InterviewConstants {

    private InterviewConstants() {
        // Utility class — not instantiable
    }

    /**
     * Grace window (minutes) granted to a candidate after scheduledStart.
     * If the candidate has not started within this window, the session is permanently
     * locked and auto-expired (No-Show Rule).
     */
    public static final long GRACE_PERIOD_MINUTES = 5L;

    /**
     * Buffer (minutes) added on top of scheduledEnd before the backend auto-closes
     * an IN_PROGRESS session whose candidate has disconnected (Disconnect Safety Rule).
     */
    public static final long ABANDON_BUFFER_MINUTES = 5L;
}
