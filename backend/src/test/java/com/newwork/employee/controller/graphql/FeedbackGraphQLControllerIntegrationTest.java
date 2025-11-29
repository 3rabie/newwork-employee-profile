package com.newwork.employee.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.Feedback;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.entity.enums.WorkLocationType;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.FeedbackRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("FeedbackGraphQLController Integration Tests")
class FeedbackGraphQLControllerIntegrationTest {

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

    private String buildGraphQLRequest(String query) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("query", query));
    }

    /**
     * Perform GraphQL POST and await async dispatch completion.
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
    @DisplayName("Should get feedback for user as author via GraphQL")
    void shouldGetFeedbackForUserAsAuthorViaGraphQL() throws Exception {
        // Given - employee1 gives feedback to employee2
        Feedback feedback = new Feedback();
        feedback.setAuthor(employee1);
        feedback.setRecipient(employee2);
        feedback.setText("Great work on the project!");
        feedback.setAiPolished(false);
        feedbackRepository.save(feedback);

        String query = String.format("""
            query {
                feedbackForUser(userId: "%s") {
                    id
                    text
                    aiPolished
                    author {
                        id
                        email
                        profile {
                            preferredName
                        }
                    }
                    recipient {
                        id
                        profile {
                            preferredName
                        }
                    }
                }
            }
            """, employee2.getId());

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackForUser", hasSize(1)))
                .andExpect(jsonPath("$.data.feedbackForUser[0].text").value("Great work on the project!"))
                .andExpect(jsonPath("$.data.feedbackForUser[0].aiPolished").value(false))
                .andExpect(jsonPath("$.data.feedbackForUser[0].author.email").value("emp1@test.com"))
                .andExpect(jsonPath("$.data.feedbackForUser[0].author.profile.preferredName").value("Test Employee 1"))
                .andExpect(jsonPath("$.data.feedbackForUser[0].recipient.profile.preferredName").value("Test Employee 2"));
    }

    @Test
    @DisplayName("Should get feedback for user as recipient via GraphQL")
    void shouldGetFeedbackForUserAsRecipientViaGraphQL() throws Exception {
        // Given - employee1 gives feedback to employee2
        Feedback feedback = new Feedback();
        feedback.setAuthor(employee1);
        feedback.setRecipient(employee2);
        feedback.setText("Excellent collaboration!");
        feedback.setAiPolished(false);
        feedbackRepository.save(feedback);

        String query = String.format("""
            query {
                feedbackForUser(userId: "%s") {
                    text
                    author {
                        profile {
                            preferredName
                        }
                    }
                }
            }
            """, employee2.getId());

        performGraphQL(employee2Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackForUser", hasSize(1)))
                .andExpect(jsonPath("$.data.feedbackForUser[0].text").value("Excellent collaboration!"))
                .andExpect(jsonPath("$.data.feedbackForUser[0].author.profile.preferredName").value("Test Employee 1"));
    }

    @Test
    @DisplayName("Should get feedback for user as manager via GraphQL")
    void shouldGetFeedbackForUserAsManagerViaGraphQL() throws Exception {
        // Given - employee1 gives feedback to employee2 (both report to manager)
        Feedback feedback = new Feedback();
        feedback.setAuthor(employee1);
        feedback.setRecipient(employee2);
        feedback.setText("Strong technical skills!");
        feedback.setAiPolished(false);
        feedbackRepository.save(feedback);

        String query = String.format("""
            query {
                feedbackForUser(userId: "%s") {
                    text
                }
            }
            """, employee2.getId());

        performGraphQL(managerToken, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackForUser", hasSize(1)))
                .andExpect(jsonPath("$.data.feedbackForUser[0].text").value("Strong technical skills!"));
    }

    @Test
    @DisplayName("Should not see feedback without permission via GraphQL")
    void shouldNotSeeFeedbackWithoutPermissionViaGraphQL() throws Exception {
        // Given - Create a third employee not managed by same manager
        User employee3 = User.builder()
                .employeeId("EMP-103")
                .email("emp3@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .manager(null)
                .build();
        employee3 = userRepository.save(employee3);
        createProfile(employee3, "Test Employee 3");
        String employee3Token = getAuthToken("emp3@test.com", "password123");

        // employee1 gives feedback to employee2
        Feedback feedback = new Feedback();
        feedback.setAuthor(employee1);
        feedback.setRecipient(employee2);
        feedback.setText("Great work!");
        feedback.setAiPolished(false);
        feedbackRepository.save(feedback);

        String query = String.format("""
            query {
                feedbackForUser(userId: "%s") {
                    text
                }
            }
            """, employee2.getId());

        performGraphQL(employee3Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackForUser", hasSize(0)));
    }

    @Test
    @DisplayName("Should get authored feedback via GraphQL")
    void shouldGetAuthoredFeedbackViaGraphQL() throws Exception {
        // Given - employee1 gives feedback to employee2
        Feedback feedback = new Feedback();
        feedback.setAuthor(employee1);
        feedback.setRecipient(employee2);
        feedback.setText("Great teamwork!");
        feedback.setAiPolished(false);
        feedbackRepository.save(feedback);

        String query = """
            query {
                myAuthoredFeedback {
                    text
                    recipient {
                        email
                        profile {
                            preferredName
                        }
                    }
                }
            }
            """;

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myAuthoredFeedback", hasSize(1)))
                .andExpect(jsonPath("$.data.myAuthoredFeedback[0].text").value("Great teamwork!"))
                .andExpect(jsonPath("$.data.myAuthoredFeedback[0].recipient.email").value("emp2@test.com"))
                .andExpect(jsonPath("$.data.myAuthoredFeedback[0].recipient.profile.preferredName").value("Test Employee 2"));
    }

    @Test
    @DisplayName("Should get received feedback via GraphQL")
    void shouldGetReceivedFeedbackViaGraphQL() throws Exception {
        // Given - employee1 gives feedback to employee2
        Feedback feedback = new Feedback();
        feedback.setAuthor(employee1);
        feedback.setRecipient(employee2);
        feedback.setText("Outstanding performance!");
        feedback.setAiPolished(false);
        feedbackRepository.save(feedback);

        String query = """
            query {
                myReceivedFeedback {
                    text
                    author {
                        email
                        profile {
                            preferredName
                        }
                    }
                }
            }
            """;

        performGraphQL(employee2Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReceivedFeedback", hasSize(1)))
                .andExpect(jsonPath("$.data.myReceivedFeedback[0].text").value("Outstanding performance!"))
                .andExpect(jsonPath("$.data.myReceivedFeedback[0].author.email").value("emp1@test.com"))
                .andExpect(jsonPath("$.data.myReceivedFeedback[0].author.profile.preferredName").value("Test Employee 1"));
    }

    @Test
    @DisplayName("Should return empty list when no feedback exists via GraphQL")
    void shouldReturnEmptyListWhenNoFeedbackExistsViaGraphQL() throws Exception {
        String query = """
            query {
                myAuthoredFeedback {
                    text
                }
            }
            """;

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myAuthoredFeedback", hasSize(0)));
    }

    @Test
    @DisplayName("Should support nested profile resolution via GraphQL (DataLoader test)")
    void shouldSupportNestedProfileResolutionViaGraphQL() throws Exception {
        // Create multiple feedback items to test DataLoader batching
        for (int i = 0; i < 5; i++) {
            Feedback feedback = new Feedback();
            feedback.setAuthor(employee1);
            feedback.setRecipient(employee2);
            feedback.setText("Feedback " + i);
            feedback.setAiPolished(false);
            feedbackRepository.save(feedback);
        }

        String query = String.format("""
            query {
                feedbackForUser(userId: "%s") {
                    text
                    author {
                        email
                        profile {
                            preferredName
                            legalFirstName
                        }
                    }
                    recipient {
                        profile {
                            preferredName
                        }
                    }
                }
            }
            """, employee2.getId());

        performGraphQL(employee1Token, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackForUser", hasSize(5)))
                .andExpect(jsonPath("$.data.feedbackForUser[0].author.profile.preferredName").value("Test Employee 1"))
                .andExpect(jsonPath("$.data.feedbackForUser[0].recipient.profile.preferredName").value("Test Employee 2"));

        // Note: DataLoader should batch all profile queries into 1-2 queries instead of N queries
        // This is verified by performance, not assertions
    }
}
