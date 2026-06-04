package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.question.AnswerRequest;
import com.aiinterview.ai_interview.dto.question.QuestionResponse;
import com.aiinterview.ai_interview.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    final QuestionService questionService;

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(questionService.getSessionQuestions(sessionId));
    }

    @GetMapping("/current")
    public ResponseEntity<QuestionResponse> getCurrentQuestion(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(questionService.getCurrentQuestion(sessionId));
    }

    @PostMapping("/{questionId}/answer")
    public ResponseEntity<QuestionResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerRequest request) {
        return ResponseEntity.ok(
                questionService.submitAnswer(sessionId, questionId, request));
    }
}