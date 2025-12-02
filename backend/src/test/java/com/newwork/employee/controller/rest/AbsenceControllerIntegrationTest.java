package com.newwork.employee.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newwork.employee.dto.request.CreateAbsenceRequest;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.AbsenceStatus;
import com.newwork.employee.entity.enums.AbsenceType;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.repository.EmployeeAbsenceRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AbsenceControllerIntegrationTest {

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
    private EmployeeAbsenceRepository absenceRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User manager;
    private User employee;
    private String managerToken;
    private String employeeToken;

    @BeforeEach
    void setUp() throws Exception {
        absenceRequestRepository.deleteAll();
        userRepository.deleteAll();

        manager = userRepository.save(User.builder()
                .employeeId("MGR-001")
                .email("manager@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.MANAGER)
                .build());

        employee = userRepository.save(User.builder()
                .employeeId("EMP-001")
                .email("employee@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build());

        managerToken = authenticate("manager@test.com");
        employeeToken = authenticate("employee@test.com");
    }

    @Test
    @DisplayName("Employee can submit request; manager can approve")
    void submitAndApproveFlow() throws Exception {
        CreateAbsenceRequest request = new CreateAbsenceRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                AbsenceType.VACATION,
                "Family trip"
        );

        ResultActions createResult = mockMvc.perform(post("/api/absence")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        String absenceId = objectMapper.readTree(createResult.andReturn().getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(patch("/api/absence/{id}", UUID.fromString(absenceId))
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AbsenceStatus.APPROVED.name()));
    }

    @Test
    @DisplayName("Non-manager cannot approve or reject")
    void nonManagerCannotApprove() throws Exception {
        CreateAbsenceRequest request = new CreateAbsenceRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                AbsenceType.SICK,
                "Flu"
        );

        ResultActions createResult = mockMvc.perform(post("/api/absence")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String absenceId = objectMapper.readTree(createResult.andReturn().getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(patch("/api/absence/{id}", UUID.fromString(absenceId))
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager can reject with note")
    void managerCanRejectWithNote() throws Exception {
        CreateAbsenceRequest request = new CreateAbsenceRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                AbsenceType.SICK,
                "Flu"
        );

        ResultActions createResult = mockMvc.perform(post("/api/absence")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String absenceId = objectMapper.readTree(createResult.andReturn().getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(patch("/api/absence/{id}", UUID.fromString(absenceId))
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REJECT\",\"note\":\"Need coverage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(AbsenceStatus.REJECTED.name()))
                .andExpect(jsonPath("$.note").value("Need coverage"));
    }

    private String authenticate(String email) throws Exception {
        var login = new com.newwork.employee.dto.request.LoginRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        var auth = objectMapper.readValue(result.getResponse().getContentAsString(),
                com.newwork.employee.dto.response.AuthResponse.class);
        return auth.getToken();
    }
}
