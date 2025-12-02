package com.newwork.employee.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.employee.dto.ProfileUpdateDTO;
import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.entity.enums.WorkLocationType;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("ProfileController Integration Tests")
class ProfileControllerIntegrationTest {

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
    private EmployeeProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User manager;
    private User employee1;
    private User employee2;
    private EmployeeProfile managerProfile;
    private EmployeeProfile employee1Profile;
    private EmployeeProfile employee2Profile;
    private String managerToken;
    private String employee1Token;
    private String employee2Token;

    @BeforeEach
    void setUp() throws Exception {
        // Clear all data
        profileRepository.deleteAll();
        userRepository.deleteAll();

        // Create manager
        manager = User.builder()
                .employeeId("MGR-100")
                .email("manager@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.MANAGER)
                .manager(null)
                .build();
        manager = userRepository.save(manager);

        // Create employees reporting to manager
        employee1 = User.builder()
                .employeeId("EMP-101")
                .email("emp1@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
        employee1 = userRepository.save(employee1);

        employee2 = User.builder()
                .employeeId("EMP-102")
                .email("emp2@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
        employee2 = userRepository.save(employee2);

        // Create profiles
        managerProfile = createProfile(manager, "Engineering", "Engineering Manager");
        employee1Profile = createProfile(employee1, "Engineering", "Senior Software Engineer");
        employee2Profile = createProfile(employee2, "Engineering", "Software Engineer");

        // Get authentication tokens
        managerToken = getAuthToken("manager@test.com", "password123");
        employee1Token = getAuthToken("emp1@test.com", "password123");
        employee2Token = getAuthToken("emp2@test.com", "password123");
    }

    private EmployeeProfile createProfile(User user, String department, String jobTitle) {
        return profileRepository.save(EmployeeProfile.builder()
                .user(user)
                .legalFirstName("First_" + user.getEmployeeId())
                .legalLastName("Last_" + user.getEmployeeId())
                .department(department)
                .jobCode("JOB-" + user.getEmployeeId())
                .jobFamily(department)
                .jobLevel("Senior")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2020, 1, 1))
                .fte(new BigDecimal("1.00"))
                .preferredName("Preferred_" + user.getEmployeeId())
                .jobTitle(jobTitle)
                .officeLocation("New York")
                .workPhone("+1-555-0100")
                .workLocationType(WorkLocationType.HYBRID)
                .bio("Bio for " + user.getEmployeeId())
                .skills("Java, Spring, React")
                .personalEmail(user.getEmail().replace("@test", ".personal@test"))
                .personalPhone("+1-555-9999")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .absenceBalanceDays(new BigDecimal("15.00"))
                .salary(new BigDecimal("100000.00"))
                .performanceRating("Exceeds Expectations")
                .build());
    }

    private String getAuthToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        return response.getToken();
    }

    // NOTE: GET endpoint tests removed - profile queries now use GraphQL
    // See ProfileGraphQLControllerIntegrationTest for GraphQL query tests

    @Test
    @DisplayName("Should update own non-sensitive fields (SELF)")
    void shouldUpdateOwnNonSensitiveFields() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .preferredName("Updated Name")
                .bio("Updated bio text")
                .workLocationType(WorkLocationType.REMOTE)
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee1.getId())
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredName").value("Updated Name"))
                .andExpect(jsonPath("$.bio").value("Updated bio text"))
                .andExpect(jsonPath("$.workLocationType").value("REMOTE"));
    }

    @Test
    @DisplayName("Should update own sensitive fields (SELF)")
    void shouldUpdateOwnSensitiveFields() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .personalEmail("newemail@personal.com")
                .personalPhone("+1-555-1234")
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee1.getId())
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalEmail").value("newemail@personal.com"))
                .andExpect(jsonPath("$.personalPhone").value("+1-555-1234"));
    }

    @Test
    @DisplayName("Should update direct report non-sensitive fields as MANAGER")
    void shouldUpdateDirectReportNonSensitiveFieldsAsManager() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .jobTitle("Lead Software Engineer")
                .officeLocation("San Francisco")
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee1.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Lead Software Engineer"))
                .andExpect(jsonPath("$.officeLocation").value("San Francisco"));
    }

    @Test
    @DisplayName("Should return 403 when MANAGER tries to update sensitive fields")
    void shouldReturn403WhenManagerTriesToUpdateSensitiveFields() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .personalEmail("manager.hacking@test.com")
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee1.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("sensitive fields")));
    }

    @Test
    @DisplayName("Should return 403 when COWORKER tries to update non-sensitive fields")
    void shouldReturn403WhenCoworkerTriesToUpdateNonSensitiveFields() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .preferredName("Hacker Name")
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee2.getId())
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @DisplayName("Should return 403 when COWORKER tries to update sensitive fields")
    void shouldReturn403WhenCoworkerTriesToUpdateSensitiveFields() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .personalEmail("coworker.hacking@test.com")
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee2.getId())
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                .personalEmail("invalid-email-format")
                .build();

        mockMvc.perform(patch("/api/profiles/{userId}", employee1.getId())
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    // NOTE: Authentication tests for GET endpoints removed
    // See ProfileGraphQLControllerIntegrationTest for GraphQL authentication tests
}
