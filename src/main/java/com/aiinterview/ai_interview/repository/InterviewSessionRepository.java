package com.aiinterview.ai_interview.repository;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByCandidateEmailIgnoreCase(String candidateEmail);

    List<InterviewSession> findByRecruiterId(Long recruiterId);

    @Query("SELECT COUNT(s) > 0 FROM InterviewSession s WHERE LOWER(s.candidateEmail) = LOWER(:email) AND s.status = :status AND s.createdAt > :cutoff")
    boolean hasCompletedSessionInLastYear(@Param("email") String email, @Param("status") SessionStatus status, @Param("cutoff") Instant cutoff);
    /**
     * Returns SCHEDULED sessions whose grace period has expired (scheduledStart + graceMinutes < now).
     * Bounded to top 100 to prevent OOM.
     */
    @Query("SELECT s FROM InterviewSession s WHERE s.status = :status AND s.scheduledStart < :cutoff")
    List<InterviewSession> findTop100ByStatusAndScheduledStartBefore(
            @Param("status") SessionStatus status,
            @Param("cutoff") Instant cutoff);

    /**
     * Returns IN_PROGRESS sessions that are considered abandoned/expired.
     * Bounded to top 100 to prevent OOM.
     */
    @Query("SELECT s FROM InterviewSession s WHERE s.status = :status AND " +
           "(s.scheduledEnd < :cutoff OR s.lastActiveAt < :cutoff OR (s.lastActiveAt IS NULL AND s.actualStart < :cutoff))")
    List<InterviewSession> findTop100ByStatusAndScheduledEndBefore(
            @Param("status") SessionStatus status,
            @Param("cutoff") Instant cutoff);
}