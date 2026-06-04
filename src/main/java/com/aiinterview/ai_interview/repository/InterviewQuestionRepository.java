package com.aiinterview.ai_interview.repository;

import com.aiinterview.ai_interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findBySessionIdOrderBySequenceOrderAsc(Long sessionId);

    Optional<InterviewQuestion> findByIdAndSessionId(Long id, Long sessionId);

    // Next unanswered question
    Optional<InterviewQuestion> findFirstBySessionIdAndCandidateAnswerIsNullOrderBySequenceOrderAsc(Long sessionId);

    // Count answered questions
    long countBySessionIdAndCandidateAnswerIsNotNull(Long sessionId);
}