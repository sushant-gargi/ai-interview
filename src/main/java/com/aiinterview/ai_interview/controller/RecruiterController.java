package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.report.ReportResponse;
import com.aiinterview.ai_interview.dto.session.SessionRequest;
import com.aiinterview.ai_interview.dto.session.SessionResponse;
import com.aiinterview.ai_interview.service.InterviewSessionService;
import com.aiinterview.ai_interview.service.ReportService;
import com.aiinterview.ai_interview.service.impl.PdfReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    private final InterviewSessionService sessionService;
    private final ReportService reportService;
    private final PdfReportService pdfReportService;

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(sessionService.createSession(request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getRecruiterSessions() {
        return ResponseEntity.ok(sessionService.getRecruiterSessions());
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{id}/report")
    public ResponseEntity<ReportResponse> getSessionReport(@PathVariable Long id) {
        ReportResponse report = reportService.getReport(id);
        return ResponseEntity.ok(report);
    }

    @GetMapping(value = "/sessions/{id}/report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadSessionReportPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfReportService.generatePdfReport(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "evaluation_report_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
