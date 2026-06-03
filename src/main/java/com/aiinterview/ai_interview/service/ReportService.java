package com.aiinterview.ai_interview.service;


import com.aiinterview.ai_interview.dto.report.ReportRequest;
import com.aiinterview.ai_interview.dto.report.ReportResponse;

public interface ReportService {
    ReportResponse saveReport(Long sessionId, ReportRequest request);
    ReportResponse getReport(Long sessionId);
}