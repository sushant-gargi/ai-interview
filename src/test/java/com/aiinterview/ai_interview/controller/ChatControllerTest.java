package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.chat.ChatEndSessionResponse;
import com.aiinterview.ai_interview.dto.chat.ChatHistoryResponse;
import com.aiinterview.ai_interview.dto.chat.ChatMessageDto;
import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class ChatControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthUtil authUtil;

    @MockitoBean
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String getValidToken() {
        User user = User.builder().id(1L).name("User").email("user@example.com").build();
        return authUtil.generateAccessToken(user);
    }

    // ==========================================
    // 1. POST /api/sessions/{sessionId}/chat (Send Message)
    // ==========================================

    @Test
    void sendMessage_validSession_returnsOk() throws Exception {
        String token = getValidToken();
        ChatResponse response = new ChatResponse(100L, "AI Response", false, "audioBase64String");

        when(chatService.sendMessage(eq(1L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(100L))
                .andExpect(jsonPath("$.assistantMessage").value("AI Response"))
                .andExpect(jsonPath("$.sessionCompleted").value(false))
                .andExpect(jsonPath("$.audioBase64").value("audioBase64String"));
    }

    @Test
    void sendMessage_followUpContext_returnsOk() throws Exception {
        String token = getValidToken();
        ChatResponse response = new ChatResponse(101L, "AI Contextual Response", false, "audioBase64Context");

        when(chatService.sendMessage(eq(1L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Follow up question content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(101L))
                .andExpect(jsonPath("$.assistantMessage").value("AI Contextual Response"));
    }

    @Test
    void sendMessage_scheduledSession_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.sendMessage(eq(1L), any(ChatRequest.class)))
                .thenThrow(new BadRequestException("Session is not in progress. Current status: SCHEDULED"));

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is not in progress. Current status: SCHEDULED"));
    }

    @Test
    void sendMessage_completedSession_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.sendMessage(eq(1L), any(ChatRequest.class)))
                .thenThrow(new BadRequestException("Session is not in progress. Current status: COMPLETED"));

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is not in progress. Current status: COMPLETED"));
    }

    @Test
    void sendMessage_blankMessage_returnsBadRequest() throws Exception {
        String token = getValidToken();

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void sendMessage_wrongUser_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.sendMessage(eq(1L), any(ChatRequest.class)))
                .thenThrow(new BadRequestException("This session does not belong to you"));

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This session does not belong to you"));
    }

    @Test
    void sendMessage_noJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/sessions/1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendMessage_twoMinutesRemainingSoftClosure_returnsOk() throws Exception {
        String token = getValidToken();
        ChatResponse response = new ChatResponse(102L, "AI soft closure warning", false, null);

        when(chatService.sendMessage(eq(1L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Almost finished\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(102L))
                .andExpect(jsonPath("$.assistantMessage").value("AI soft closure warning"));
    }

    // ==========================================
    // 2. GET /api/sessions/{sessionId}/chat (Get Chat History)
    // ==========================================

    @Test
    void getChatHistory_validSession_returnsOk() throws Exception {
        String token = getValidToken();
        Instant now = Instant.now();
        ChatHistoryResponse response = new ChatHistoryResponse(1L, "IN_PROGRESS", List.of(
                new ChatMessageDto("USER", "hello", now)
        ));

        when(chatService.getChatHistory(eq(1L))).thenReturn(response);

        mockMvc.perform(get("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[0].message").value("hello"));
    }

    @Test
    void getChatHistory_systemMessagesFiltered_returnsOk() throws Exception {
        String token = getValidToken();
        Instant now = Instant.now();
        ChatHistoryResponse response = new ChatHistoryResponse(1L, "IN_PROGRESS", List.of(
                new ChatMessageDto("USER", "hello", now),
                new ChatMessageDto("ASSISTANT", "hi", now)
        ));

        when(chatService.getChatHistory(eq(1L))).thenReturn(response);

        mockMvc.perform(get("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"));
    }

    @Test
    void getChatHistory_wrongUser_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.getChatHistory(eq(1L)))
                .thenThrow(new BadRequestException("This session does not belong to you"));

        mockMvc.perform(get("/api/sessions/1/chat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This session does not belong to you"));
    }

    @Test
    void getChatHistory_nonExistentSession_returnsNotFound() throws Exception {
        String token = getValidToken();
        when(chatService.getChatHistory(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Session", "999"));

        mockMvc.perform(get("/api/sessions/999/chat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session with id 999 not found"));
    }

    // ==========================================
    // 3. POST /api/sessions/{sessionId}/chat/end (End Session)
    // ==========================================

    @Test
    void endSession_validSession_returnsOk() throws Exception {
        String token = getValidToken();
        ChatEndSessionResponse response = new ChatEndSessionResponse(1L, "COMPLETED", "Interview session ended successfully.");

        when(chatService.endSession(eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/sessions/1/chat/end")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.message").value("Interview session ended successfully."));
    }

    @Test
    void endSession_alreadyCompleted_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.endSession(eq(1L)))
                .thenThrow(new BadRequestException("Session is not in progress. Current status: COMPLETED"));

        mockMvc.perform(post("/api/sessions/1/chat/end")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is not in progress. Current status: COMPLETED"));
    }

    @Test
    void endSession_scheduledSession_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.endSession(eq(1L)))
                .thenThrow(new BadRequestException("Session is not in progress. Current status: SCHEDULED"));

        mockMvc.perform(post("/api/sessions/1/chat/end")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is not in progress. Current status: SCHEDULED"));
    }

    @Test
    void endSession_wrongUser_returnsBadRequest() throws Exception {
        String token = getValidToken();
        when(chatService.endSession(eq(1L)))
                .thenThrow(new BadRequestException("This session does not belong to you"));

        mockMvc.perform(post("/api/sessions/1/chat/end")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This session does not belong to you"));
    }

    @Test
    void endSession_noJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/sessions/1/chat/end"))
                .andExpect(status().isUnauthorized());
    }
}
