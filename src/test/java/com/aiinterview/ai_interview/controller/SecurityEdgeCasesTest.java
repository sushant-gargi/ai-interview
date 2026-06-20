package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.security.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SecurityEdgeCasesTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private AuthUtil authUtil;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ==========================================
    // JWT Edge Cases
    // ==========================================

    @Test
    void anyProtectedEndpoint_expiredJwt_returnsUnauthorized() throws Exception {
        when(authUtil.verifyAccessToken("expired-token"))
                .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "JWT expired"));

        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid JWT token: JWT expired"));
    }

    @Test
    void anyProtectedEndpoint_malformedJwt_returnsUnauthorized() throws Exception {
        when(authUtil.verifyAccessToken("malformed-token"))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("JWT malformed"));

        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid JWT token: JWT malformed"));
    }

    @Test
    void anyProtectedEndpoint_wrongSecretJwt_returnsUnauthorized() throws Exception {
        when(authUtil.verifyAccessToken("wrong-secret-token"))
                .thenThrow(new io.jsonwebtoken.security.SignatureException("JWT signature does not match"));

        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer wrong-secret-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid JWT token: JWT signature does not match"));
    }

    // ==========================================
    // Public Endpoints (Bypass Auth)
    // ==========================================

    @Test
    void swaggerUi_accessibleWithoutAuth_returnsOkOrRedirect() throws Exception {
        // Swagger UI should be publicly accessible.
        // It might return a redirect (3xx) or OK (2xx).
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void actuatorHealth_accessibleWithoutAuth_returnsNotFound() throws Exception {
        // Actuator health should be publicly accessible.
        // Since actuator is not enabled as a dependency, it should bypass security and return 404, not 401.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }
}
