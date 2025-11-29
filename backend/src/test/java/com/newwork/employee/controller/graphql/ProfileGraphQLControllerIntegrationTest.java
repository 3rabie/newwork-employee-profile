package com.newwork.employee.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("ProfileGraphQLController Integration Tests")
class ProfileGraphQLControllerIntegrationTest {

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
    private String managerToken;
    private String employee1Token;

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
        createProfile(manager, "Engineering", "Engineering Manager");
        createProfile(employee1, "Engineering", "Senior Software Engineer");
        createProfile(employee2, "Engineering", "Software Engineer");

        // Get authentication tokens
        managerToken = getAuthToken("manager@test.com", "password123");
        employee1Token = getAuthToken("emp1@test.com", "password123");
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

    private String buildGraphQLRequest(String query) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("query", query));
    }

    /**
     * Perform GraphQL POST and wait for async dispatch to complete.
     */
    private ResultActions performGraphQL(String token, String query) throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildGraphQLRequest(query)))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result));
    }

    @Test
    @DisplayName("Should get own profile with all fields via GraphQL (SELF)")
    void shouldGetOwnProfileWithAllFieldsViaGraphQL() throws Exception {
        String query = String.format("""
            query {
                profile(userId: "%s") {
                    userId
                    legalFirstName
                    legalLastName
                    preferredName
                    department
                    jobTitle
                    personalEmail
                    salary
                    performanceRating
                    metadata {
                        relationship
                        visibleFields
                        editableFields
                    }
                }
            }
            """, employee1.getId());

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.userId").value(employee1.getId().toString()))
                .andExpect(jsonPath("$.data.profile.legalFirstName").value("First_EMP-101"))
                .andExpect(jsonPath("$.data.profile.legalLastName").value("Last_EMP-101"))
                .andExpect(jsonPath("$.data.profile.preferredName").value("Preferred_EMP-101"))
                .andExpect(jsonPath("$.data.profile.department").value("Engineering"))
                .andExpect(jsonPath("$.data.profile.jobTitle").value("Senior Software Engineer"))
                // SENSITIVE fields visible to SELF
                .andExpect(jsonPath("$.data.profile.personalEmail").value("emp1.personal@test.com"))
                .andExpect(jsonPath("$.data.profile.salary").value(100000.00))
                .andExpect(jsonPath("$.data.profile.performanceRating").value("Exceeds Expectations"))
                // Metadata
                .andExpect(jsonPath("$.data.profile.metadata.relationship").value("SELF"))
                .andExpect(jsonPath("$.data.profile.metadata.visibleFields").isArray())
                .andExpect(jsonPath("$.data.profile.metadata.editableFields").isArray());
    }

    @Test
    @DisplayName("Should get direct report profile with sensitive fields via GraphQL (MANAGER)")
    void shouldGetDirectReportProfileWithSensitiveFieldsViaGraphQL() throws Exception {
        String query = String.format("""
            query {
                profile(userId: "%s") {
                    userId
                    legalFirstName
                    preferredName
                    department
                    jobTitle
                    personalEmail
                    salary
                    metadata {
                        relationship
                    }
                }
            }
            """, employee1.getId());

        performGraphQL(managerToken, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.userId").value(employee1.getId().toString()))
                .andExpect(jsonPath("$.data.profile.legalFirstName").value("First_EMP-101"))
                .andExpect(jsonPath("$.data.profile.preferredName").value("Preferred_EMP-101"))
                .andExpect(jsonPath("$.data.profile.department").value("Engineering"))
                .andExpect(jsonPath("$.data.profile.jobTitle").value("Senior Software Engineer"))
                // SENSITIVE fields visible to MANAGER for direct reports
                .andExpect(jsonPath("$.data.profile.personalEmail").value("emp1.personal@test.com"))
                .andExpect(jsonPath("$.data.profile.salary").value(100000.00))
                .andExpect(jsonPath("$.data.profile.metadata.relationship").value("MANAGER"));
    }

    @Test
    @DisplayName("Should get coworker profile without sensitive fields via GraphQL (COWORKER)")
    void shouldGetCoworkerProfileWithoutSensitiveFieldsViaGraphQL() throws Exception {
        String query = String.format("""
            query {
                profile(userId: "%s") {
                    userId
                    legalFirstName
                    preferredName
                    department
                    jobTitle
                    personalEmail
                    salary
                    performanceRating
                    metadata {
                        relationship
                    }
                }
            }
            """, employee2.getId());

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.userId").value(employee2.getId().toString()))
                .andExpect(jsonPath("$.data.profile.legalFirstName").value("First_EMP-102"))
                .andExpect(jsonPath("$.data.profile.preferredName").value("Preferred_EMP-102"))
                .andExpect(jsonPath("$.data.profile.department").value("Engineering"))
                .andExpect(jsonPath("$.data.profile.jobTitle").value("Software Engineer"))
                // SENSITIVE fields NOT visible to COWORKER
                .andExpect(jsonPath("$.data.profile.personalEmail").doesNotExist())
                .andExpect(jsonPath("$.data.profile.salary").doesNotExist())
                .andExpect(jsonPath("$.data.profile.performanceRating").doesNotExist())
                .andExpect(jsonPath("$.data.profile.metadata.relationship").value("OTHER"));
    }

    @Test
    @DisplayName("Should return error when profile not found via GraphQL")
    void shouldReturnErrorWhenProfileNotFoundViaGraphQL() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();

        String query = String.format("""
            query {
                profile(userId: "%s") {
                    userId
                    legalFirstName
                }
            }
            """, nonExistentUserId);

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].message").value(containsString("Profile not found")))
                .andExpect(jsonPath("$.data.profile").value(nullValue()));
    }

    @Test
    @DisplayName("Should return error when not authenticated via GraphQL")
    void shouldReturnErrorWhenNotAuthenticatedViaGraphQL() throws Exception {
        String query = String.format("""
            query {
                profile(userId: "%s") {
                    userId
                    legalFirstName
                }
            }
            """, employee1.getId());

        // Spring Security blocks unauthenticated requests with 403 before reaching GraphQL
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildGraphQLRequest(query)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return error when token is invalid via GraphQL")
    void shouldReturnErrorWhenTokenIsInvalidViaGraphQL() throws Exception {
        String query = String.format("""
            query {
                profile(userId: "%s") {
                    userId
                    legalFirstName
                }
            }
            """, employee1.getId());

        // Spring Security blocks invalid tokens with 403 before reaching GraphQL
        mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildGraphQLRequest(query)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should support flexible field selection via GraphQL")
    void shouldSupportFlexibleFieldSelectionViaGraphQL() throws Exception {
        // Request only specific fields
        String query = String.format("""
            query {
                profile(userId: "%s") {
                    preferredName
                    jobTitle
                }
            }
            """, employee1.getId());

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.preferredName").value("Preferred_EMP-101"))
                .andExpect(jsonPath("$.data.profile.jobTitle").value("Senior Software Engineer"))
                // Other fields not requested, should not be in response
                .andExpect(jsonPath("$.data.profile.legalFirstName").doesNotExist())
                .andExpect(jsonPath("$.data.profile.department").doesNotExist());
    }
}
