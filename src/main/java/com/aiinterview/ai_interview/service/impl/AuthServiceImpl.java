package com.aiinterview.ai_interview.service.impl;


import com.aiinterview.ai_interview.dto.auth.AuthResponse;
import com.aiinterview.ai_interview.dto.auth.LoginRequest;
import com.aiinterview.ai_interview.dto.auth.SignupRequest;
import com.aiinterview.ai_interview.entity.User;
import com.aiinterview.ai_interview.error.BadRequestException;
import com.aiinterview.ai_interview.repository.UserRepository;
import com.aiinterview.ai_interview.security.AuthUtil;
import com.aiinterview.ai_interview.service.AuthService;
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
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);

        String token = authUtil.generateAccessToken(user);
        return new AuthResponse(token, user.getName(), user.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate credentials via the manager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // 2. Extract the fully loaded User entity straight from the authenticated principal (Just like lovable-clone!)
        User user = (User) authentication.getPrincipal();

        // 3. Generate the token using the authenticated user object
        String token = authUtil.generateAccessToken(user);

        // 4. Return the populated response
        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}