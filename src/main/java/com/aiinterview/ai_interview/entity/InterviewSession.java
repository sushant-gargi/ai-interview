package com.aiinterview.ai_interview.entity;

import com.aiinterview.ai_interview.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "interview_sessions", indexes = {
        @Index(name = "idx_status_start", columnList = "status, scheduled_start"),
        @Index(name = "idx_status_end", columnList = "status, scheduled_end")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    User recruiter;

    @Column(nullable = false)
    String candidateEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = true)
    Resume resume;

    String jobRole;

    @Column(columnDefinition = "TEXT")
    String jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SessionStatus status;

    Instant scheduledStart;
    Instant scheduledEnd;
    Instant actualStart;
    Instant actualEnd;
    Instant lastActiveAt;

    @CreationTimestamp
    Instant createdAt;
}