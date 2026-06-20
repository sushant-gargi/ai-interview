package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.chat.ChatEndSessionResponse;
import com.aiinterview.ai_interview.dto.chat.ChatHistoryResponse;
import com.aiinterview.ai_interview.dto.chat.ChatMessageDto;
import com.aiinterview.ai_interview.dto.chat.ChatRequest;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.entity.ConversationMessage;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.repository.ConversationMessageRepository;
import com.aiinterview.ai_interview.repository.InterviewSessionRepository;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ChatService;
import com.aiinterview.ai_interview.service.ReportService;
import com.aiinterview.ai_interview.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ConversationMessageRepository messageRepository;
    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final ChatClient chatClient;
    private final ReportService reportService;
    private final TtsService ttsService;

    @Override
    @Transactional
    public void initializeSession(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        String resumeText = session.getResume() != null && session.getResume().getParsedText() != null
                ? session.getResume().getParsedText() : "";
        String jobDesc = session.getJobDescription() != null ? session.getJobDescription() : "";

        // Core priming system prompt — instructs conversational deep-dives and adaptive cross-questioning
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
                6. CRITICAL — VOICE INTERVIEW FORMAT: Every single response you give MUST be short and concise.
                   A maximum of 2–3 sentences total. The candidate is responding by voice, so long
                   multi-part questions are impossible to answer in a single spoken answer.
                   NEVER use bullet lists, numbered lists, or markdown in your responses.
                   ONE thought, ONE question, end of response.
                7. Do NOT summarize the candidate's answer back to them before asking the next question.
                   Go directly to your follow-up or next question.
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

        // Initial organic greeting from the AI agent
        String assistantHello = "Hello! I am your AI technical interviewer for the " + session.getJobRole() + " position. "
                + "I have reviewed your resume along with our target job requirements. Let's start with a brief introduction. "
                + "Could you please tell me about yourself and outline the core technical frameworks you specialize in?";

        ConversationMessage assistantMsg = ConversationMessage.builder()
                .session(session)
                .role("ASSISTANT")
                .content(assistantHello)
                .build();
        messageRepository.save(assistantMsg);

        log.info("Initialized AI chat session for ID: {}", sessionId);
    }

    @Override
    public ChatResponse sendMessage(Long sessionId, ChatRequest request) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = getSessionAndValidateOwner(sessionId, userId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Session is not in progress. Current status: " + session.getStatus());
        }

        // Update activity timestamp
        session.setLastActiveAt(Instant.now());
        sessionRepository.save(session);

        // 1. Save and immediately flush the user's message so it's part of the history query
        ConversationMessage userMsg = ConversationMessage.builder()
                .session(session)
                .role("USER")
                .content(request.message())
                .build();
        messageRepository.save(userMsg);
        messageRepository.flush();

        // 2. Fetch full dialogue history including the just-flushed user message
        List<ConversationMessage> dbHistory = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Map to Spring AI message types — mutable list so we can inject the time prompt
        List<Message> springAiMessages = new ArrayList<>(dbHistory.stream()
                .map(msg -> switch (msg.getRole().toUpperCase()) {
                    case "SYSTEM" -> (Message) new SystemMessage(msg.getContent());
                    case "ASSISTANT" -> (Message) new AssistantMessage(msg.getContent());
                    default -> (Message) new UserMessage(msg.getContent());
                })
                .toList());

        // 3. Inject transient time-awareness for pacing and soft closure (Strict Duration Compliance)
        if (session.getScheduledEnd() != null) {
            long minutesRemaining = Duration.between(Instant.now(), session.getScheduledEnd()).toMinutes();
            if (minutesRemaining >= 0) {
                String timeWarning = String.format(
                        "SYSTEM RULE: There are %d minutes remaining in this interview slot. " +
                        "If the time remaining is 2 minutes or less, you MUST immediately issue a soft closure " +
                        "warning indicating time is almost up and ask if they have any final questions. " +
                        "Do NOT ask any new technical questions.",
                        minutesRemaining
                );
                springAiMessages.add(new SystemMessage(timeWarning));
            }
        }

        log.info("Sending {} messages to Spring AI for session {}", springAiMessages.size(), sessionId);

        // 4. Call LLM
        String assistantResponse;
        try {
            assistantResponse = chatClient.prompt()
                    .messages(springAiMessages)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Spring AI call failed for session {}: ", sessionId, e);
            throw new RuntimeException("AI model connection timed out. Please try sending your answer again.");
        }

        if (assistantResponse == null || assistantResponse.isBlank()) {
            assistantResponse = "That sounds interesting. Could you break down your implementation approach further?";
        }

        // 5. Persist the AI's response and flush
        ConversationMessage assistantMsg = ConversationMessage.builder()
                .session(session)
                .role("ASSISTANT")
                .content(assistantResponse)
                .build();
        ConversationMessage savedAssistant = messageRepository.save(assistantMsg);
        messageRepository.flush();

        // 6. Generate speech audio for the response
        String audioBase64 = null;
        byte[] audioBytes = ttsService.generateSpeech(assistantResponse);
        if (audioBytes != null) {
            audioBase64 = java.util.Base64.getEncoder().encodeToString(audioBytes);
        }

        return new ChatResponse(savedAssistant.getId(), assistantResponse, false, audioBase64);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = getSessionAndValidateOwner(sessionId, userId);

        List<ChatMessageDto> messages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .filter(m -> !m.getRole().equalsIgnoreCase("SYSTEM")) // exclude internal system prompts from client view
                .map(m -> new ChatMessageDto(m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();

        log.info("Retrieved {} messages for session {}", messages.size(), sessionId);
        return new ChatHistoryResponse(sessionId, session.getStatus().name(), messages);
    }

    @Override
    @Transactional
    public ChatEndSessionResponse endSession(Long sessionId) {
        Long userId = authUtil.getCurrentUserId();
        InterviewSession session = getSessionAndValidateOwner(sessionId, userId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Session is not in progress. Current status: " + session.getStatus());
        }

        // Mark session as completed
        session.setStatus(SessionStatus.COMPLETED);
        session.setActualEnd(Instant.now());
        sessionRepository.save(session);

        log.info("Session {} marked as COMPLETED by user", sessionId);

        // Auto-generate the evaluation report — fire and catch so a report failure
        // never rolls back the session state transition
        try {
            reportService.generateCompletedReport(sessionId);
        } catch (Exception e) {
            log.error("Report generation failed for completed session {}. Report can be retried via GET /api/reports/{}.", sessionId, sessionId, e);
        }

        return new ChatEndSessionResponse(sessionId, "COMPLETED", "Interview session ended successfully. Your evaluation report is being compiled.");
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private InterviewSession getSessionAndValidateOwner(Long sessionId, Long userId) {
        com.aiinterview.ai_interview.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId.toString()));

        if (!session.getCandidateEmail().equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("This session does not belong to you");
        }

        return session;
    }

    private String truncate(String s, int limit) {
        if (s == null) return "";
        return s.length() <= limit ? s : s.substring(0, limit) + "...";
    }
}