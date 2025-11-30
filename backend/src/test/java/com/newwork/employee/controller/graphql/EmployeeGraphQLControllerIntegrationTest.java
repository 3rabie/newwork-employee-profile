package com.newwork.employee.controller.graphql;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EmployeeGraphQLControllerIntegrationTest {

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
    private User engineer;
    private User productManager;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        profileRepository.deleteAll();
        userRepository.deleteAll();

        manager = createUser("mgr@test.com", "MGR-001", Role.MANAGER, null);
        engineer = createUser("engineer@test.com", "EMP-101", Role.EMPLOYEE, manager);
        productManager = createUser("pm@test.com", "EMP-202", Role.EMPLOYEE, null);

        profileRepository.save(createProfile(manager, "Engineering", "Director of Engineering"));
        profileRepository.save(createProfile(engineer, "Engineering", "Software Engineer"));
        profileRepository.save(createProfile(productManager, "Product", "Product Manager"));

        managerToken = authenticate("mgr@test.com");
    }

    @Test
    @DisplayName("Manager should see coworkers and direct reports via directory query")
    void shouldReturnCoworkerDirectory() throws Exception {
        String query = """
            query {
                coworkerDirectory {
                    userId
                    preferredName
                    department
                    relationship
                    directReport
                }
            }
            """;

        performGraphQL(managerToken, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coworkerDirectory", hasSize(2)))
                .andExpect(jsonPath("$.data.coworkerDirectory[?(@.userId=='%s')].directReport",
                        engineer.getId().toString()).value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.data.coworkerDirectory[?(@.userId=='%s')].relationship",
                        productManager.getId().toString()).value(org.hamcrest.Matchers.hasItem("OTHER")));
    }

    @Test
    @DisplayName("Directory query should support search filter")
    void shouldFilterDirectoryBySearch() throws Exception {
        String query = """
            query {
                coworkerDirectory(search: "product") {
                    userId
                    department
                }
            }
            """;

        performGraphQL(managerToken, query)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coworkerDirectory", hasSize(1)))
                .andExpect(jsonPath("$.data.coworkerDirectory[0].userId")
                        .value(productManager.getId().toString()));
    }

    private ResultActions performGraphQL(String token, String query) throws Exception {
        MvcResult result = mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("query", query))))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result));
    }

    private String authenticate(String email) throws Exception {
        LoginRequest request = new LoginRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getToken();
    }

    private User createUser(String email, String employeeId, Role role, User manager) {
        User user = User.builder()
                .email(email)
                .employeeId(employeeId)
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .manager(manager)
                .build();
        return userRepository.save(user);
    }

    private EmployeeProfile createProfile(User user, String department, String jobTitle) {
        return profileRepository.save(EmployeeProfile.builder()
                .user(user)
                .legalFirstName("First_" + user.getEmployeeId())
                .legalLastName("Last_" + user.getEmployeeId())
                .preferredName("Pref_" + user.getEmployeeId())
                .department(department)
                .jobTitle(jobTitle)
                .jobCode("JOB-" + user.getEmployeeId())
                .jobFamily(department)
                .jobLevel("Senior")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2020, 1, 1))
                .fte(new BigDecimal("1.00"))
                .workLocationType(WorkLocationType.HYBRID)
                .build());
    }
}
