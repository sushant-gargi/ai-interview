package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ChatControllerUnitTest {

    MockMvc mockMvc;
    ChatService chatService;

    @BeforeEach
    void setup() {
        chatService = Mockito.mock(ChatService.class);
        ChatController controller = new ChatController(chatService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void sendMessage_returnsAssistantReply() throws Exception {
        ChatResponse resp = new ChatResponse(123L, "Thanks for your answer.", false);
        Mockito.when(chatService.sendMessage(eq(1L), any(ChatRequest.class))).thenReturn(resp);

        String body = "{\"message\":\"Hello interviewer\"}";

        mockMvc.perform(post("/api/sessions/1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"messageId\":123,\"assistantMessage\":\"Thanks for your answer.\",\"sessionCompleted\":false}"));
    }
}

