package com.aiinterview.ai_interview.entity;

import com.aiinterview.ai_interview.dto.report.QuestionScore;
import com.aiinterview.ai_interview.enums.HiringRecommendation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "reports")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    InterviewSession session;

    @Column(columnDefinition = "TEXT")
    String summary;

    Integer overallScore;

    @Enumerated(EnumType.STRING)
    HiringRecommendation recommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    Map<String, Integer> skillBreakdown;

    @JdbcTypeCode(SqlTypes.JSON)
    List<QuestionScore> questionScores;

    @CreationTimestamp
    Instant generatedAt;
}