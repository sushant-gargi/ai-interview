package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.resume.ResumeResponse;
import com.aiinterview.ai_interview.dto.session.SessionRequest;
import com.aiinterview.ai_interview.dto.session.SessionResponse;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.enums.SessionStatus;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.InterviewSessionService;
import com.aiinterview.ai_interview.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
public class InterviewSessionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    AuthUtil authUtil;

    @MockitoBean
    InterviewSessionService sessionService;

    @MockitoBean
    ResumeService resumeService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private String getCandidateToken() {
        User user = User.builder().id(1L).name("User").email("user@example.com").role(com.aiinterview.ai_interview.entity.Role.CANDIDATE).build();
        return authUtil.generateAccessToken(user);
    }

    private String getRecruiterToken() {
        User user = User.builder().id(2L).name("Recruiter").email("recruiter@example.com").role(com.aiinterview.ai_interview.entity.Role.RECRUITER).build();
        return authUtil.generateAccessToken(user);
    }

    // ==========================================
    // 1. POST /api/recruiter/sessions (Create Session)
    // ==========================================

    @Test
    void createSession_validFutureTimeResumeExists_returnsOk() throws Exception {
        String token = getRecruiterToken();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        SessionResponse response = new SessionResponse(10L, "SDET", "JD", SessionStatus.SCHEDULED, start, end, null, null, Instant.now(), true);
        when(sessionService.createSession(any(SessionRequest.class))).thenReturn(response);

        String body = String.format("{\"jobRole\":\"SDET\",\"jobDescription\":\"JD\",\"candidateEmail\":\"candidate@test.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}", start, end);

        mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.resumeUploaded").value(true));
    }

    @Test
    void createSession_noResumeUploadedYet_returnsOk() throws Exception {
        String token = getRecruiterToken();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        SessionResponse response = new SessionResponse(10L, "SDET", "JD", SessionStatus.SCHEDULED, start, end, null, null, Instant.now(), false);
        when(sessionService.createSession(any(SessionRequest.class))).thenReturn(response);

        String body = String.format("{\"jobRole\":\"SDET\",\"jobDescription\":\"JD\",\"candidateEmail\":\"candidate@test.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}", start, end);

        mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.resumeUploaded").value(false));
    }

    @Test
    void createSession_endBeforeStart_returnsBadRequest() throws Exception {
        String token = getRecruiterToken();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.minusSeconds(100);

        when(sessionService.createSession(any(SessionRequest.class))).thenThrow(new BadRequestException("Scheduled end time must be after start time"));

        String body = String.format("{\"jobRole\":\"SDET\",\"jobDescription\":\"JD\",\"candidateEmail\":\"candidate@test.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}", start, end);

        mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Scheduled end time must be after start time"));
    }

    @Test
    void createSession_blankJobRole_returnsBadRequest() throws Exception {
        String token = getRecruiterToken();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        String body = String.format("{\"jobRole\":\"\",\"jobDescription\":\"JD\",\"candidateEmail\":\"candidate@test.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}", start, end);

        mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void createSession_missingScheduledEnd_returnsBadRequest() throws Exception {
        String token = getRecruiterToken();
        Instant start = Instant.now().plusSeconds(3600);

        String body = String.format("{\"jobRole\":\"SDET\",\"jobDescription\":\"JD\",\"candidateEmail\":\"candidate@test.com\",\"scheduledStart\":\"%s\"}", start);

        mockMvc.perform(post("/api/recruiter/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void createSession_noJwt_returnsUnauthorized() throws Exception {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        String body = String.format("{\"jobRole\":\"SDET\",\"jobDescription\":\"JD\",\"candidateEmail\":\"candidate@test.com\",\"scheduledStart\":\"%s\",\"scheduledEnd\":\"%s\"}", start, end);

        mockMvc.perform(post("/api/recruiter/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. POST /api/sessions/{id}/resume (Lobby Upload)
    // ==========================================

    @Test
    void uploadResume_scheduledSessionValid_returnsOk() throws Exception {
        String token = getCandidateToken();
        ResumeResponse response = new ResumeResponse(2L, "resume.pdf", "text", Instant.now());
        when(resumeService.uploadResumeForSession(eq(1L), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        mockMvc.perform(multipart("/api/sessions/1/resume")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.fileName").value("resume.pdf"));
    }

    @Test
    void uploadResume_wrongUserSession_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(resumeService.uploadResumeForSession(eq(1L), any())).thenThrow(new BadRequestException("This session does not belong to you"));

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        mockMvc.perform(multipart("/api/sessions/1/resume")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This session does not belong to you"));
    }

    @Test
    void uploadResume_inProgressSession_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(resumeService.uploadResumeForSession(eq(1L), any())).thenThrow(new BadRequestException("Resume can only be uploaded for SCHEDULED sessions"));

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        mockMvc.perform(multipart("/api/sessions/1/resume")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Resume can only be uploaded for SCHEDULED sessions"));
    }

    @Test
    void uploadResume_nonPdf_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(resumeService.uploadResumeForSession(eq(1L), any())).thenThrow(new BadRequestException("Only PDF files are accepted"));

        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", MediaType.TEXT_PLAIN_VALUE, "text content".getBytes());

        mockMvc.perform(multipart("/api/sessions/1/resume")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PDF files are accepted"));
    }

    @Test
    void uploadResume_nonExistentSession_returnsNotFound() throws Exception {
        String token = getCandidateToken();
        when(resumeService.uploadResumeForSession(eq(999L), any())).thenThrow(new ResourceNotFoundException("Session", "999"));

        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());

        mockMvc.perform(multipart("/api/sessions/999/resume")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session with id 999 not found"));
    }

    // ==========================================
    // 3. POST /api/sessions/{id}/start (Start Session)
    // ==========================================

    @Test
    void startSession_validStart_returnsOk() throws Exception {
        String token = getCandidateToken();
        Instant now = Instant.now();
        SessionResponse response = new SessionResponse(1L, "SDET", "JD", SessionStatus.IN_PROGRESS, now, now.plusSeconds(3600), now, null, now, true);
        when(sessionService.startSession(1L)).thenReturn(response);

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resumeUploaded").value(true));
    }

    @Test
    void startSession_beforeScheduledStart_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("Interview has not started yet. Scheduled at: tomorrow"));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interview has not started yet. Scheduled at: tomorrow"));
    }

    @Test
    void startSession_noResumeUploaded_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("Resume is required. Please upload your resume in the lobby before starting the interview."));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Resume is required. Please upload your resume in the lobby before starting the interview."));
    }

    @Test
    void startSession_gracePeriodExpired_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("Interview grace period has expired"));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Interview grace period has expired"));
    }

    @Test
    void startSession_alreadyInProgress_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("Session is already in progress"));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is already in progress"));
    }

    @Test
    void startSession_alreadyCompleted_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("Session is already completed"));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is already completed"));
    }

    @Test
    void startSession_expiredStatus_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("Session has expired and can no longer be started"));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session has expired and can no longer be started"));
    }

    @Test
    void startSession_wrongUser_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(1L)).thenThrow(new BadRequestException("This session does not belong to you"));

        mockMvc.perform(post("/api/sessions/1/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This session does not belong to you"));
    }

    @Test
    void startSession_nonExistentSession_returnsNotFound() throws Exception {
        String token = getCandidateToken();
        when(sessionService.startSession(999L)).thenThrow(new ResourceNotFoundException("Session", "999"));

        mockMvc.perform(post("/api/sessions/999/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session with id 999 not found"));
    }

    // ==========================================
    // 4. GET /api/sessions (Get My Sessions)
    // ==========================================

    @Test
    void getMySessions_valid_returnsList() throws Exception {
        String token = getCandidateToken();
        Instant now = Instant.now();
        SessionResponse s = new SessionResponse(1L, "SDET", "JD", SessionStatus.SCHEDULED, now, now.plusSeconds(3600), null, null, now, false);
        when(sessionService.getMySessions()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].jobRole").value("SDET"));
    }

    @Test
    void getMySessions_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMySessions_emptyList_returnsEmptyArray() throws Exception {
        String token = getCandidateToken();
        when(sessionService.getMySessions()).thenReturn(List.of());

        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ==========================================
    // 5. POST /api/sessions/{id}/heartbeat (Heartbeat)
    // ==========================================

    @Test
    void heartbeat_validSession_returnsOk() throws Exception {
        String token = getCandidateToken();
        doNothing().when(sessionService).recordHeartbeat(1L);

        mockMvc.perform(post("/api/sessions/1/heartbeat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(sessionService, times(1)).recordHeartbeat(1L);
    }

    @Test
    void heartbeat_scheduledSession_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        doThrow(new BadRequestException("Heartbeat can only be recorded for IN_PROGRESS sessions"))
                .when(sessionService).recordHeartbeat(1L);

        mockMvc.perform(post("/api/sessions/1/heartbeat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Heartbeat can only be recorded for IN_PROGRESS sessions"));
    }

    @Test
    void heartbeat_completedSession_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        doThrow(new BadRequestException("Heartbeat can only be recorded for IN_PROGRESS sessions"))
                .when(sessionService).recordHeartbeat(1L);

        mockMvc.perform(post("/api/sessions/1/heartbeat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Heartbeat can only be recorded for IN_PROGRESS sessions"));
    }

    @Test
    void heartbeat_wrongUserSession_returnsBadRequest() throws Exception {
        String token = getCandidateToken();
        doThrow(new BadRequestException("This session does not belong to you"))
                .when(sessionService).recordHeartbeat(1L);

        mockMvc.perform(post("/api/sessions/1/heartbeat")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This session does not belong to you"));
    }
}
