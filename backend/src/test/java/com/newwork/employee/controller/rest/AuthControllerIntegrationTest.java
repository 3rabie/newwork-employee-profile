package com.newwork.employee.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.request.SwitchUserRequest;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests the complete authentication flow including security configuration.
 * Uses Testcontainers for real PostgreSQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testManager;
    private User testEmployee1;
    private User testEmployee2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create test manager
        testManager = User.builder()
                .id(UUID.randomUUID())
                .employeeId("TST_MGR_001")
                .email("test.manager@testcompany.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .role(Role.MANAGER)
                .manager(null)
                .build();
        testManager = userRepository.save(testManager);

        // Create test employees reporting to test manager
        testEmployee1 = User.builder()
                .id(UUID.randomUUID())
                .employeeId("TST_EMP_001")
                .email("test.employee1@testcompany.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .role(Role.EMPLOYEE)
                .manager(testManager)
                .build();
        testEmployee1 = userRepository.save(testEmployee1);

        testEmployee2 = User.builder()
                .id(UUID.randomUUID())
                .employeeId("TST_EMP_002")
                .email("test.employee2@testcompany.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .role(Role.EMPLOYEE)
                .manager(testManager)
                .build();
        testEmployee2 = userRepository.save(testEmployee2);
    }

    @Test
    void login_WithValidManagerCredentials_ReturnsTokenAndUserDetails() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "test.manager@testcompany.com",
                "TestPass123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("test.manager@testcompany.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.employeeId").value("TST_MGR_001"))
                .andExpect(jsonPath("$.userId").value(testManager.getId().toString()))
                .andExpect(jsonPath("$.managerId").isEmpty());
    }

    @Test
    void login_WithValidEmployeeCredentials_ReturnsTokenWithManagerId() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "test.employee1@testcompany.com",
                "TestPass123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test.employee1@testcompany.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.employeeId").value("TST_EMP_001"))
                .andExpect(jsonPath("$.userId").value(testEmployee1.getId().toString()))
                .andExpect(jsonPath("$.managerId").value(testManager.getId().toString()));
    }

    @Test
    void login_WithInvalidPassword_ReturnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "test.manager@testcompany.com",
                "WrongPassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithNonExistentEmail_ReturnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "nonexistent@testcompany.com",
                "TestPass123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithEmptyEmail_ReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "",
                "TestPass123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithEmptyPassword_ReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "test.manager@testcompany.com",
                ""
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithInvalidEmailFormat_ReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "not-an-email",
                "TestPass123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void switchUser_WithValidEmployeeEmail_ReturnsNewToken() throws Exception {
        SwitchUserRequest request = new SwitchUserRequest("test.employee1@testcompany.com");

        mockMvc.perform(post("/api/auth/switch-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test.employee1@testcompany.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.userId").value(testEmployee1.getId().toString()));
    }

    @Test
    void switchUser_WithValidManagerEmail_ReturnsManagerToken() throws Exception {
        SwitchUserRequest request = new SwitchUserRequest("test.manager@testcompany.com");

        mockMvc.perform(post("/api/auth/switch-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test.manager@testcompany.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.managerId").isEmpty());
    }

    @Test
    void switchUser_WithNonExistentEmail_ReturnsNotFound() throws Exception {
        SwitchUserRequest request = new SwitchUserRequest("nonexistent@testcompany.com");

        mockMvc.perform(post("/api/auth/switch-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void switchUser_WithEmptyEmail_ReturnsBadRequest() throws Exception {
        SwitchUserRequest request = new SwitchUserRequest("");

        mockMvc.perform(post("/api/auth/switch-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void switchUser_WithInvalidEmailFormat_ReturnsBadRequest() throws Exception {
        SwitchUserRequest request = new SwitchUserRequest("not-an-email");

        mockMvc.perform(post("/api/auth/switch-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithAllTestUsers_AllReturnValidTokens() throws Exception {
        String[] testData = {
                "test.manager@testcompany.com",
                "test.employee1@testcompany.com",
                "test.employee2@testcompany.com"
        };

        for (String email : testData) {
            LoginRequest loginRequest = new LoginRequest(email, "TestPass123!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.email").value(email));
        }
    }

    @Test
    void login_GeneratedTokenContainsExpectedClaims() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "test.employee1@testcompany.com",
                "TestPass123!"
        );

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify token structure (JWT has 3 parts separated by dots)
        String token = objectMapper.readTree(responseBody).get("token").asText();
        String[] tokenParts = token.split("\\.");
        assert tokenParts.length == 3 : "JWT token should have 3 parts (header.payload.signature)";
    }
}
