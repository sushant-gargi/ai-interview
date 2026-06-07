package com.aiinterview.ai_interview.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "conversation_messages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    InterviewSession session;

    // Roles: SYSTEM, USER, ASSISTANT
    @Column(nullable = false)
    String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    @CreationTimestamp
    Instant createdAt;
}

