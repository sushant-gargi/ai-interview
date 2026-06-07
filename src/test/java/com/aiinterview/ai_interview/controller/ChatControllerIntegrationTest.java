package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ChatControllerIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @MockitoBean
    ChatService chatService;

    @Test
    void postChatEndpoint_returnsOk() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        when(chatService.sendMessage(eq(5L), any())).thenReturn(new ChatResponse(10L, "ok", false));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/sessions/5/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk());
    }
}