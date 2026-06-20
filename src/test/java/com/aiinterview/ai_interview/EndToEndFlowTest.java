package com.aiinterview.ai_interview;

import com.aiinterview.ai_interview.dto.auth.AuthResponse;
import com.aiinterview.ai_interview.dto.chat.ChatResponse;
import com.aiinterview.ai_interview.dto.session.SessionResponse;
import com.aiinterview.ai_interview.entity.InterviewSession;
import com.aiinterview.ai_interview.entity.Report;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.repository.*;
import com.aiinterview.ai_interview.scheduling.SessionCleanupScheduler;
import com.aiinterview.ai_interview.service.TtsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class EndToEndFlowTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SessionCleanupScheduler sessionCleanupScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ChatClient chatClient;

    @MockitoBean
    private TtsService ttsService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @MockitoBean
    private com.aiinterview.ai_interview.service.EmailService emailService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Truncate tables with CASCADE to handle foreign key references from unmapped tables (e.g., interview_questions)
        jdbcTemplate.execute("TRUNCATE TABLE conversation_messages, reports, interview_sessions, resumes, users RESTART IDENTITY CASCADE");

        // Setup ChatClient mock builder chain
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        String mockJsonResponse = "{" +
                "\"overallScore\": 8," +
                "\"recommendation\": \"YES\"," +
                "\"summary\": \"Mock AI Report Summary\"," +
                "\"skillBreakdown\": {\"Technical\": 8}," +
                "\"questionScores\": []" +
                "}";
        when(responseSpec.content()).thenReturn(mockJsonResponse);
        
        // Mock TTS to return null or dummy audio bytes
        when(ttsService.generateSpeech(anyString())).thenReturn(null);
    }

    private byte[] createValidPdfBytes() {
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String signupAndGetToken(String name, String email, String role) throws Exception {
        String signupBody = String.format(
                "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"password123\"}",
                name, email);
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isOk())
                .andReturn();
        
        if ("RECRUITER".equals(role)) {
            User user = userRepository.findByEmail(email).orElseThrow();
            user.setRole(com.aiinterview.ai_interview.entity.Role.RECRUITER);
            userRepository.save(user);
            
            String loginBody = String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email);
            result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isOk())
                    .andReturn();
        }
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    // ==========================================
    // E2E Flow 1: Signup -> Login -> Full Flow -> Report
    //
    // Recruiter creates a session for a candidate.
    // Candidate uploads resume, starts, chats, ends, and fetches report.
    // ==========================================

    @Test
    void fullE2eFlow_happyPath_completesSuccessfully() throws Exception {
        byte[] pdfBytes = createValidPdfBytes();

        // 1. Recruiter signs up
        String recruiterToken = signupAndGetToken("Recruiter E2E", "recruiter_e2e@example.com", "RECRUITER");
        assertThat(recruiterToken).isNotBlank();

        // 2. Candidate signs up
        String candidateToken = signupAndGetToken("Candidate E2E", "candidate_e2e@example.com", "CANDIDATE");
        assertThat(candidateToken).isNotBlank();

        // 3. Recruiter creates a session for the candidate
        //    Start time 1 minute in the past → within the 5-minute grace window so candidate can join immediately
        Instant start = Instant.now().minusSeconds(60);
        Instant end = start.plusSeconds(3600);
        String sessionBody = String.format(
                "{\"jobRole\":\"SDET 1\",\"jobDescription\":\"Must know Java and MockMvc\",\"candidateEmail\":\"candidate_e2e@example.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}",
                start, end
        );
        MvcResult sessionResult = mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isOk())
                .andReturn();

        SessionResponse sessionResponse = objectMapper.readValue(sessionResult.getResponse().getContentAsString(), SessionResponse.class);
        Long sessionId = sessionResponse.id();

        // 4. Candidate uploads lobby resume
        MockMultipartFile lobbyResume = new MockMultipartFile("file", "lobby_resume.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes);
        mockMvc.perform(multipart("/api/sessions/" + sessionId + "/resume")
                        .file(lobbyResume)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        // 5. Candidate starts session
        mockMvc.perform(post("/api/sessions/" + sessionId + "/start")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 6. Candidate chats x3
        for (int i = 1; i <= 3; i++) {
            String chatBody = "{\"message\":\"Candidate response message " + i + "\"}";
            mockMvc.perform(post("/api/sessions/" + sessionId + "/chat")
                            .header("Authorization", "Bearer " + candidateToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(chatBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assistantMessage").exists());
        }

        // 7. Candidate ends session
        mockMvc.perform(post("/api/sessions/" + sessionId + "/chat/end")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify session status is updated in db
        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);

        // 8. Recruiter fetches report
        mockMvc.perform(get("/api/recruiter/sessions/" + sessionId + "/report")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.summary").value("Mock AI Report Summary"))
                .andExpect(jsonPath("$.overallScore").value(8))
                .andExpect(jsonPath("$.recommendation").value("YES"));
    }

    // ==========================================
    // E2E Flow 2: Grace Period Expiration (No Show)
    // ==========================================

    @Test
    void gracePeriodE2eFlow_candidateNoShow_autoExpires() throws Exception {
        byte[] pdfBytes = createValidPdfBytes();

        // Recruiter and Candidate sign up
        String recruiterToken = signupAndGetToken("No Show Recruiter", "noshow_recruiter@example.com", "RECRUITER");
        String candidateToken = signupAndGetToken("No Show Candidate", "noshow_candidate@example.com", "CANDIDATE");

        // Recruiter creates a session in the future
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);
        String sessionBody = String.format(
                "{\"jobRole\":\"SDET 1\",\"jobDescription\":\"JD\",\"candidateEmail\":\"noshow_candidate@example.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}",
                start, end
        );
        MvcResult sessionResult = mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andReturn();

        System.out.println("DEBUG GRACE PERIOD CREATE SESSION STATUS: " + sessionResult.getResponse().getStatus());
        System.out.println("DEBUG GRACE PERIOD CREATE SESSION BODY: " + sessionResult.getResponse().getContentAsString());

        assertThat(sessionResult.getResponse().getStatus()).isEqualTo(200);

        SessionResponse sessionResponse = objectMapper.readValue(sessionResult.getResponse().getContentAsString(), SessionResponse.class);
        Long sessionId = sessionResponse.id();

        // Candidate uploads lobby resume
        MockMultipartFile lobbyResume = new MockMultipartFile("file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes);
        mockMvc.perform(multipart("/api/sessions/" + sessionId + "/resume")
                        .file(lobbyResume)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        // Backdate the session's scheduledStart to 6 minutes in the past (past the 5-minute grace period)
        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setScheduledStart(Instant.now().minus(Duration.ofMinutes(6)));
        sessionRepository.save(session);

        // Candidate tries to start session → should throw BadRequestException
        mockMvc.perform(post("/api/sessions/" + sessionId + "/start")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interview grace period has expired. You must join within 5 minutes of the scheduled start time."));

        // Verify session status is EXPIRED in the database
        session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.EXPIRED);

        // Verify No-Show report is compiled
        Report report = reportRepository.findBySessionId(sessionId).orElseThrow();
        assertThat(report.getSummary()).contains("Candidate failed to join");
    }

    // ==========================================
    // E2E Flow 3: Disconnect Safety Cleanup
    // ==========================================

    @Test
    void disconnectSafetyE2eFlow_sessionAbandoned_autoCompletes() throws Exception {
        byte[] pdfBytes = createValidPdfBytes();

        // Recruiter and Candidate sign up
        String recruiterToken = signupAndGetToken("Abandoned Recruiter", "abandoned_recruiter@example.com", "RECRUITER");
        String candidateToken = signupAndGetToken("Abandoned Candidate", "abandoned_candidate@example.com", "CANDIDATE");

        // Recruiter creates a session
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        String sessionBody = String.format(
                "{\"jobRole\":\"SDET 1\",\"jobDescription\":\"JD\",\"candidateEmail\":\"abandoned_candidate@example.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}",
                start, end
        );
        MvcResult sessionResult = mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andReturn();

        System.out.println("DEBUG DISCONNECT CREATE SESSION STATUS: " + sessionResult.getResponse().getStatus());
        System.out.println("DEBUG DISCONNECT CREATE SESSION BODY: " + sessionResult.getResponse().getContentAsString());

        assertThat(sessionResult.getResponse().getStatus()).isEqualTo(200);

        SessionResponse sessionResponse = objectMapper.readValue(sessionResult.getResponse().getContentAsString(), SessionResponse.class);
        Long sessionId = sessionResponse.id();

        // Candidate uploads lobby resume
        MockMultipartFile lobbyResume = new MockMultipartFile("file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes);
        mockMvc.perform(multipart("/api/sessions/" + sessionId + "/resume")
                        .file(lobbyResume)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        // Candidate starts session via API
        mockMvc.perform(post("/api/sessions/" + sessionId + "/start")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        // Verify session is IN_PROGRESS
        InterviewSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);

        // Simulate candidate disconnecting and scheduledEnd passing by 6 minutes (buffer is 5 minutes)
        session.setScheduledEnd(Instant.now().minus(Duration.ofMinutes(6)));
        sessionRepository.save(session);

        // Manually run cleanup scheduler task
        sessionCleanupScheduler.cleanupAbandonedSessions();

        // Verify session transitioned to COMPLETED
        session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);

        // Verify report is generated
        Report report = reportRepository.findBySessionId(sessionId).orElseThrow();
        assertThat(report.getSummary()).contains("Mock AI Report Summary");
        assertThat(report.getOverallScore()).isEqualTo(8);
    }
}
