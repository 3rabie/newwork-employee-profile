package com.newwork.employee.controller.rest;

import com.newwork.employee.config.properties.SecurityProperties;
import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.request.SwitchUserRequest;
import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Provides login and user switching functionality.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs for user login and role switching")
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties securityProperties;

    /**
     * Authenticate user with email and password
     *
     * @param loginRequest contains user credentials (email and password)
     * @return AuthResponse with JWT token and user details
     */
    @Operation(
            summary = "User Login",
            description = "Authenticate user with email and password. Returns JWT token and user profile information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body (validation error)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for email: {}", loginRequest.getEmail());
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Switch to a different user account (Demo feature)
     *
     * @param request contains email of user to switch to
     * @return AuthResponse with JWT token and user details
     */
    @Operation(
            summary = "Switch User",
            description = "Demo feature that allows switching to any user account without password authentication. " +
                    "This is useful for testing different user roles (EMPLOYEE vs MANAGER)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully switched user",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body (validation error)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found with provided email",
                    content = @Content
            )
    })
    @PostMapping("/switch-user")
    public ResponseEntity<AuthResponse> switchUser(@Valid @RequestBody SwitchUserRequest request) {
        if (!securityProperties.getDemo().isSwitchUserEnabled()) {
            throw new ForbiddenException("Switch-user feature is disabled");
        }
        log.info("Switch user request received for email: {}", request.getEmail());
        AuthResponse response = authService.switchUser(request.getEmail());
        return ResponseEntity.ok(response);
    }
}
