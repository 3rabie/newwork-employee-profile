package com.newwork.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.employee.dto.request.CreateFeedbackRequest;
import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.request.PolishFeedbackRequest;
import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.dto.response.PolishFeedbackResponse;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.entity.enums.WorkLocationType;
import com.newwork.employee.exception.AiServiceException;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.FeedbackRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.FeedbackPolishService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("FeedbackController Integration Tests")
class FeedbackControllerIntegrationTest {

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
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private FeedbackPolishService feedbackPolishService;

    private User manager;
    private User employee1;
    private User employee2;
    private String managerToken;
    private String employee1Token;
    private String employee2Token;

    @BeforeEach
    void setUp() throws Exception {
        // Clear all data
        feedbackRepository.deleteAll();
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
        createProfile(manager, "Test Manager");
        createProfile(employee1, "Test Employee 1");
        createProfile(employee2, "Test Employee 2");

        // Get authentication tokens
        managerToken = getAuthToken("manager@test.com", "password123");
        employee1Token = getAuthToken("emp1@test.com", "password123");
        employee2Token = getAuthToken("emp2@test.com", "password123");
    }

    private void createProfile(User user, String preferredName) {
        profileRepository.save(EmployeeProfile.builder()
                .user(user)
                .legalFirstName("First")
                .legalLastName("Last")
                .department("Engineering")
                .jobCode("JOB-001")
                .jobFamily("Engineering")
                .jobLevel("Senior")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2020, 1, 1))
                .fte(new BigDecimal("1.00"))
                .preferredName(preferredName)
                .jobTitle("Engineer")
                .officeLocation("New York")
                .workPhone("+1-555-0100")
                .workLocationType(WorkLocationType.HYBRID)
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

    @Test
    @DisplayName("Should create feedback successfully")
    void shouldCreateFeedbackSuccessfully() throws Exception {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setRecipientId(employee2.getId());
        request.setText("Great work on the project!");
        request.setAiPolished(false);

        // When/Then
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.authorId").value(employee1.getId().toString()))
                .andExpect(jsonPath("$.authorName").value("Test Employee 1"))
                .andExpect(jsonPath("$.recipientId").value(employee2.getId().toString()))
                .andExpect(jsonPath("$.recipientName").value("Test Employee 2"))
                .andExpect(jsonPath("$.text").value("Great work on the project!"))
                .andExpect(jsonPath("$.aiPolished").value(false))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should fail to create feedback to self")
    void shouldFailToCreateFeedbackToSelf() throws Exception {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setRecipientId(employee1.getId()); // Same as author
        request.setText("I'm great!");

        // When/Then
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail to create feedback without authentication")
    void shouldFailToCreateFeedbackWithoutAuth() throws Exception {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        request.setRecipientId(employee2.getId());
        request.setText("Great work!");

        // When/Then
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403 for missing auth token
    }

    // NOTE: GET endpoint tests removed - feedback queries now use GraphQL
    // See FeedbackGraphQLControllerIntegrationTest for GraphQL query tests
    // - feedbackForUser query
    // - myAuthoredFeedback query
    // - myReceivedFeedback query

    @Test
    @DisplayName("Should validate required fields when creating feedback")
    void shouldValidateRequiredFieldsWhenCreating() throws Exception {
        // Given - Invalid request with missing fields
        CreateFeedbackRequest request = new CreateFeedbackRequest();
        // recipientId and text are null

        // When/Then
        mockMvc.perform(post("/api/feedback")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should polish feedback text using AI service")
    void shouldPolishFeedback() throws Exception {
        when(feedbackPolishService.polish("Great teamwork from the whole team!"))
                .thenReturn(new PolishFeedbackResponse("Great teamwork from the whole team!", "Great teamwork from the whole team! Keep it up."));

        PolishFeedbackRequest request = new PolishFeedbackRequest();
        request.setText("Great teamwork from the whole team!");

        mockMvc.perform(post("/api/feedback/polish")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalText").value("Great teamwork from the whole team!"))
                .andExpect(jsonPath("$.polishedText").value("Great teamwork from the whole team! Keep it up."));
    }

    @Test
    @DisplayName("Should return bad request when polishing text shorter than 10 characters")
    void shouldValidatePolishRequest() throws Exception {
        mockMvc.perform(post("/api/feedback/polish")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 502 when AI service fails")
    void shouldHandleAiServiceFailure() throws Exception {
        when(feedbackPolishService.polish(anyString()))
                .thenThrow(new AiServiceException("AI unavailable"));

        mockMvc.perform(post("/api/feedback/polish")
                        .header("Authorization", "Bearer " + employee1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great teamwork from the whole team!\"}"))
                .andExpect(status().isBadGateway());
    }
}
