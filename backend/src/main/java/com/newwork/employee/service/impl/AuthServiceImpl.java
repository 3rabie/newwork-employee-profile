package com.newwork.employee.service.impl;

import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.entity.User;
import com.newwork.employee.exception.InvalidCredentialsException;
import com.newwork.employee.exception.UserNotFoundException;
import com.newwork.employee.mapper.AuthMapper;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.security.JwtTokenProvider;
import com.newwork.employee.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of authentication service.
 * Handles user login and role switching for demo purposes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthMapper authMapper;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + loginRequest.getEmail()));

            String token = jwtTokenProvider.generateToken(user);

            log.info("Login successful for user: {}", user.getEmail());
            return authMapper.toAuthResponse(user, token);

        } catch (AuthenticationException e) {
            log.error("Authentication failed for email: {}", loginRequest.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse switchUser(String email) {
        log.info("Switching to user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        String token = jwtTokenProvider.generateToken(user);

        log.info("Switched to user: {}", user.getEmail());
        return authMapper.toAuthResponse(user, token);
    }
}
