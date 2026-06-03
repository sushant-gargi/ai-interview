package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.report.ReportRequest;
import com.aiinterview.ai_interview.dto.report.ReportResponse;
import com.aiinterview.ai_interview.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    final ReportService reportService;

    @PostMapping("/{sessionId}")
    public ResponseEntity<ReportResponse> saveReport(
            @PathVariable Long sessionId,
            @RequestBody ReportRequest request) {
        return ResponseEntity.ok(reportService.saveReport(sessionId, request));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(reportService.getReport(sessionId));
    }
}