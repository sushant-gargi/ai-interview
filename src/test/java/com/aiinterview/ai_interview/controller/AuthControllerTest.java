package com.aiinterview.ai_interview.controller;

import com.aiinterview.ai_interview.dto.auth.AuthResponse;
import com.aiinterview.ai_interview.dto.auth.LoginRequest;
import com.aiinterview.ai_interview.dto.auth.SignupRequest;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    AuthService authService;

    // --- Signup Tests ---

    @Test
    void signup_validUser_returnsOk() throws Exception {
        AuthResponse response = new AuthResponse("mock-token", "Jane Doe", "jane@example.com");
        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        String body = "{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void signup_duplicateEmail_returnsBadRequest() throws Exception {
        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new BadRequestException("Email already registered: duplicate@example.com"));

        String body = "{\"name\":\"Jane Doe\",\"email\":\"duplicate@example.com\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered: duplicate@example.com"));
    }

    @Test
    void signup_invalidEmailFormat_returnsBadRequest() throws Exception {
        String body = "{\"name\":\"Jane Doe\",\"email\":\"invalid-email\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void signup_blankName_returnsBadRequest() throws Exception {
        String body = "{\"name\":\"\",\"email\":\"jane@example.com\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void signup_passwordTooShort_returnsBadRequest() throws Exception {
        String body = "{\"name\":\"Jane Doe\",\"email\":\"jane@example.com\",\"password\":\"123\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void signup_missingAllFields_returnsBadRequest() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    // --- Login Tests ---

    @Test
    void login_correctCredentials_returnsOk() throws Exception {
        AuthResponse response = new AuthResponse("mock-token", "Jane Doe", "jane@example.com");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        String body = "{\"email\":\"jane@example.com\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        String body = "{\"email\":\"jane@example.com\",\"password\":\"wrong\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication failed: Bad credentials"));
    }

    @Test
    void login_nonExistentEmail_returnsNotFound() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UsernameNotFoundException("jane@example.com"));

        String body = "{\"email\":\"jane@example.com\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: jane@example.com"));
    }

    @Test
    void login_blankEmail_returnsBadRequest() throws Exception {
        String body = "{\"email\":\"\",\"password\":\"password\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Input Validation Failed"));
    }

    @Test
    void login_malformedJson_returnsBadRequest() throws Exception {
        String body = "{\"email\":\"jane@example.com\", \"password\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
