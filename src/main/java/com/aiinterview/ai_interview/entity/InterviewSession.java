package com.aiinterview.ai_interview.entity;

import com.aiinterview.ai_interview.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "interview_sessions")
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
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
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

    @CreationTimestamp
    Instant createdAt;
}