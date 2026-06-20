package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.resume.ResumeResponse;
import com.aiinterview.ai_interview.dto.session.SessionRequest;
import com.aiinterview.ai_interview.dto.session.SessionResponse;
import com.aiinterview.ai_interview.service.InterviewSessionService;
import com.aiinterview.ai_interview.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class InterviewSessionController {

    final InterviewSessionService sessionService;
    final ResumeService resumeService;

    // Candidate endpoints

    @PostMapping("/{id}/resume")
    public ResponseEntity<ResumeResponse> uploadResume(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(resumeService.uploadResumeForSession(id, file));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<SessionResponse> startSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.startSession(id));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getMySessions() {
        return ResponseEntity.ok(sessionService.getMySessions());
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> recordHeartbeat(@PathVariable Long id) {
        sessionService.recordHeartbeat(id);
        return ResponseEntity.ok().build();
    }
}