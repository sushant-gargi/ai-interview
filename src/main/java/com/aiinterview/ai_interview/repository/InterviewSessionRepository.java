package com.aiinterview.ai_interview.repository;
import com.aiinterview.ai_interview.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUserId(Long userId);
}