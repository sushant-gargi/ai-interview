package com.aiinterview.ai_interview.service.impl;

import com.aiinterview.ai_interview.dto.auth.AuthResponse;
import com.aiinterview.ai_interview.dto.auth.LoginRequest;
import com.aiinterview.ai_interview.dto.auth.SignupRequest;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.AuthService;
import com.aiinterview.ai_interview.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final AuthUtil authUtil;
    final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse signup(SignupRequest request) {
        String normalizedEmail = request.email().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email already registered: " + normalizedEmail);
        }

        Role userRole = Role.CANDIDATE;
        if (request.role() != null && request.role().equalsIgnoreCase("RECRUITER")) {
            userRole = Role.RECRUITER;
        }

        User user = User.builder()
                .name(request.name())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role(userRole)
                .build();

        try {
            userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException("Email already registered: " + normalizedEmail);
        }

        String token = authUtil.generateAccessToken(user);
        return new AuthResponse(token, user.getName(), user.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate credentials via the manager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        // 2. Extract the fully loaded User entity from the authenticated principal
        User user = (User) authentication.getPrincipal();

        // 3. Generate the token using the authenticated user object
        String token = authUtil.generateAccessToken(user);

        // 4. Return the populated response
        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}