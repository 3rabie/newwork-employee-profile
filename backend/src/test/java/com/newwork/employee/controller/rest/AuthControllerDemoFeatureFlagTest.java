package com.newwork.employee.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Objects;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test ensuring switch-user endpoint respects the demo feature flag.
 */
@SpringBootTest(properties = "app.security.demo.switch-user-enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerDemoFeatureFlagTest {

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

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User manager = User.builder()
                .id(UUID.randomUUID())
                .employeeId("MGR-900")
                .email("switch.manager@test.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .role(Role.MANAGER)
                .build();
        userRepository.save(Objects.requireNonNull(manager));
    }

    @Test
    void switchUser_WhenFeatureDisabled_ReturnsForbidden() throws Exception {
        SwitchUserRequest request = new SwitchUserRequest("switch.manager@test.com");

        mockMvc.perform(post("/api/auth/switch-user")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden());
    }
}
