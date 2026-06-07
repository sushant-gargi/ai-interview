package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/chat")
@RequiredArgsConstructor
public class ChatController {

    final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(sessionId, request);
        return ResponseEntity.ok(response);
    }
}

