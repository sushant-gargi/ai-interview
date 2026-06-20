package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.report.ReportResponse;
import com.aiinterview.ai_interview.entity.Role;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.enums.HiringRecommendation;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.error.ResourceNotFoundException;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for report retrieval via the role-secured recruiter endpoint:
 * GET /api/recruiter/sessions/{id}/report
 *
 * The old GET /api/reports/{sessionId} endpoint has been removed because it was
 * accessible by any authenticated user regardless of role, allowing session ID
 * guessing to expose evaluation reports to unauthorized parties.
 */
@SpringBootTest
public class ReportControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthUtil authUtil;

    @MockitoBean
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /** Generates a RECRUITER-role token for use in tests against the recruiter endpoint. */
    private String getRecruiterToken() {
        User user = User.builder().id(1L).name("Recruiter").email("recruiter@example.com").role(Role.RECRUITER).build();
        return authUtil.generateAccessToken(user);
    }

    /** Generates a CANDIDATE-role token to verify role enforcement. */
    private String getCandidateToken() {
        User user = User.builder().id(2L).name("Candidate").email("candidate@example.com").role(Role.CANDIDATE).build();
        return authUtil.generateAccessToken(user);
    }

    // ==========================================
    // GET /api/recruiter/sessions/{id}/report
    // ==========================================

    @Test
    void getReport_asRecruiter_afterSessionEndedNormally_returnsOk() throws Exception {
        String token = getRecruiterToken();
        ReportResponse response = new ReportResponse(
                100L, 1L, "SDET", "Summary content", 8,
                HiringRecommendation.YES, Map.of("Technical", 8), List.of(), Instant.now()
        );

        when(reportService.getReport(eq(1L))).thenReturn(response);

        mockMvc.perform(get("/api/recruiter/sessions/1/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.sessionId").value(1L))
                .andExpect(jsonPath("$.jobRole").value("SDET"))
                .andExpect(jsonPath("$.summary").value("Summary content"));
    }

    @Test
    void getReport_asRecruiter_noShowSession_returnsOk() throws Exception {
        String token = getRecruiterToken();
        ReportResponse response = new ReportResponse(
                101L, 2L, "Backend Dev", "No-show report summary", 0,
                HiringRecommendation.NO, Map.of(), List.of(), Instant.now()
        );

        when(reportService.getReport(eq(2L))).thenReturn(response);

        mockMvc.perform(get("/api/recruiter/sessions/2/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.summary").value("No-show report summary"));
    }

    @Test
    void getReport_asRecruiter_abandonedSession_returnsOk() throws Exception {
        String token = getRecruiterToken();
        ReportResponse response = new ReportResponse(
                102L, 3L, "Frontend Dev", "Abandoned session report summary", 0,
                HiringRecommendation.NO, Map.of(), List.of(), Instant.now()
        );

        when(reportService.getReport(eq(3L))).thenReturn(response);

        mockMvc.perform(get("/api/recruiter/sessions/3/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(102L))
                .andExpect(jsonPath("$.summary").value("Abandoned session report summary"));
    }

    @Test
    void getReport_asRecruiter_sessionNotYetEnded_returnsBadRequest() throws Exception {
        String token = getRecruiterToken();
        when(reportService.getReport(eq(1L)))
                .thenThrow(new BadRequestException("Report is not generated because the session has not ended"));

        mockMvc.perform(get("/api/recruiter/sessions/1/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Report is not generated because the session has not ended"));
    }

    @Test
    void getReport_asRecruiter_nonExistentSession_returnsNotFound() throws Exception {
        String token = getRecruiterToken();
        when(reportService.getReport(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Report", "999"));

        mockMvc.perform(get("/api/recruiter/sessions/999/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Report with id 999 not found"));
    }

    @Test
    void getReport_asCandidate_returnsForbidden() throws Exception {
        // Candidates must NOT access the recruiter report endpoint — enforced by @PreAuthorize
        String candidateToken = getCandidateToken();

        mockMvc.perform(get("/api/recruiter/sessions/1/report")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReport_noJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/recruiter/sessions/1/report"))
                .andExpect(status().isUnauthorized());
    }
}
