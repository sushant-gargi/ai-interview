package com.aiinterview.ai_interview.repository;
import com.aiinterview.ai_interview.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("SELECT r FROM Report r JOIN FETCH r.session WHERE r.session.id = :sessionId")
    Optional<Report> findBySessionId(@Param("sessionId") Long sessionId);
}