package com.aiinterview.ai_interview.repository;

import com.aiinterview.ai_interview.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
}