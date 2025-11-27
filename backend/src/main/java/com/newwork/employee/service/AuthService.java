package com.newwork.employee.service;

import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.response.AuthResponse;

/**
 * Service contract for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticate user with email and password
     *
     * @param loginRequest containing email and password
     * @return AuthResponse with JWT token and user details
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Switch to a different user (demo feature for testing)
     *
     * @param email of the user to switch to
     * @return AuthResponse with new JWT token and user details
     */
    AuthResponse switchUser(String email);
}
