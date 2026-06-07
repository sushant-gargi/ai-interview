package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.entity.ConversationMessage;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.ConversationMessageRepository;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ConversationMessageRepository messageRepository;
    private final InterviewSessionRepository sessionRepository;
    private final AuthUtil authUtil;
    private final ChatClient chatClient; // Spring AI ChatClient client entrypoint

    @Override
    @Transactional
    public void initializeSession(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        String resumeText = session.getResume() != null && session.getResume().getParsedText() != null
                ? session.getResume().getParsedText() : "";
        String jobDesc = session.getJobDescription() != null ? session.getJobDescription() : "";

        // Core priming system prompt instructing conversational deep-dives and adaptive cross-questioning
        String systemContent = """
                You are an elite technical AI Interviewer evaluating a candidate for the role: %s.
                
                ## Objective
                Conduct a natural, rigorous, and adaptive interview. Do NOT read from a static checklist.
                Proactively probe the candidate's depth of knowledge by reading their responses
                and challenging them with realistic technical follow-up questions.
                
                ## Context
                Job Description:
                %s
                
                Candidate Resume:
                %s
                
                ## Rules
                1. Greet the candidate and set clear expectations for the round.
                2. Ask exactly ONE clear question at a time.
                3. When the candidate answers, analyze it for depth. If the answer is generic,
                   challenge them ("Why did you choose that?", "What are the edge cases?").
                4. Keep a professional, objective tone throughout.
                5. After the greeting, always start by asking the candidate to introduce themselves.
                """.formatted(
                session.getJobRole(),
                truncate(jobDesc, 3000),
                truncate(resumeText, 3000)
        );

        ConversationMessage systemMsg = ConversationMessage.builder()
                .session(session)
                .role("SYSTEM")
                .content(systemContent)
                .build();
        messageRepository.save(systemMsg);

        // Initial organic greeting from the AI agent to capture customer engagement flow
        String assistantHello = "Hello! I am your AI technical interviewer for the " + session.getJobRole() + " position. "
                + "I have reviewed your resume along with our target job requirements. Let's start with a brief introduction. "
                + "Could you please tell me about yourself and outline the core technical frameworks you specialize in?";

        ConversationMessage assistantMsg = ConversationMessage.builder()
                .session(session)
                .role("ASSISTANT")
                .content(assistantHello)
                .build();
        messageRepository.save(assistantMsg);

        log.info("Initialized context primed AI chat session for ID: {}", sessionId);
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(Long sessionId, ChatRequest request) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (!session.getUser().getId().equals(userId)) {
            throw new BadRequestException("This session does not belong to you");
        }

        if (session.getStatus() == null || !session.getStatus().name().equals("IN_PROGRESS")) {
            throw new BadRequestException("Session is not in progress. Current status: " + session.getStatus());
        }

        // 1. Build and Save user response
        ConversationMessage userMsg = ConversationMessage.builder()
                .session(session)
                .role("USER")
                .content(request.message())
                .build();
        messageRepository.save(userMsg);

        // 👇 FORCE FLUSH: Push row to PostgreSQL immediately so it's readable for our history
        messageRepository.flush();

        // 2. Query entire historical dialogue including the flushed message
        List<ConversationMessage> dbHistory = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<Message> springAiMessages = dbHistory.stream().map(msg -> {
            switch (msg.getRole().toUpperCase()) {
                case "SYSTEM": return new SystemMessage(msg.getContent());
                case "ASSISTANT": return new AssistantMessage(msg.getContent());
                default: return new UserMessage(msg.getContent());
            }
        }).collect(Collectors.toCollection(ArrayList::new));

        log.info("Sending historical transcript of {} messages to Spring AI...", springAiMessages.size());

        String assistantResponse;
        try {
            // 3. Request next evaluation step from AI model
            assistantResponse = chatClient.prompt()
                    .messages(springAiMessages)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Spring AI call failed directly: ", e);
            throw new RuntimeException("AI model connection timed out. Please try sending your answer again.");
        }

        if (assistantResponse == null || assistantResponse.isBlank()) {
            assistantResponse = "That sounds interesting. Could you break down your implementation approach further?";
        }


        // 4. Save and flush the new AI cross-question back to history
        ConversationMessage assistantMsg = ConversationMessage.builder()
                .session(session)
                .role("ASSISTANT")
                .content(assistantResponse)
                .build();
        ConversationMessage savedAssistant = messageRepository.save(assistantMsg);
        messageRepository.flush();

        return new ChatResponse(savedAssistant.getId(), assistantResponse, false);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSummaryFromTranscript(Long sessionId) {
        // Leverages Spring AI to create an advanced analytical evaluation summary of the transcript
        List<ConversationMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        String transcript = messages.stream()
                .filter(m -> !m.getRole().equals("SYSTEM"))
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String summaryPrompt = """
                You are a senior technical talent evaluator. Analyze the following interview dialogue transcript carefully.
                Write a concise, structured evaluation summary (3-4 sentences max) detailing the candidate's core technical strengths,
                notable architectural knowledge gaps, and overall communication clarity displayed during the session.
                
                Transcript Content:
                %s
                """.formatted(transcript);

        return chatClient.prompt()
                .user(summaryPrompt)
                .call()
                .content();
    }

    private String truncate(String s, int limit) {
        if (s == null) return "";
        return s.length() <= limit ? s : s.substring(0, limit) + "...";
    }
}