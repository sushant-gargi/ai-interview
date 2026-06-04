package com.aiinterview.ai_interview.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    InterviewSession session;

    @Column(nullable = false, columnDefinition = "TEXT")
    String questionText;

    // Order in which question is asked — 1, 2, 3...
    @Column(nullable = false)
    Integer sequenceOrder;

    // Candidate's answer — null means unanswered
    @Column(columnDefinition = "TEXT")
    String candidateAnswer;

    // AI feedback on the answer — filled in Phase 2
    @Column(columnDefinition = "TEXT")
    String aiFeedback;

    // Score out of 10 given by AI — filled in Phase 2
    Integer score;

    // Ideal answer shown after evaluation — filled in Phase 2
    @Column(columnDefinition = "TEXT")
    String idealAnswer;

    // When the candidate submitted the answer
    Instant answeredAt;

    @CreationTimestamp
    Instant createdAt;
}