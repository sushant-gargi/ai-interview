package com.aiinterview.ai_interview.repository;

import com.aiinterview.ai_interview.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserId(Long userId);
    Optional<Resume> findTopByUserIdOrderByUploadedAtDesc(Long userId);
}