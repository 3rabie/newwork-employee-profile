package com.newwork.employee.service;

import com.newwork.employee.dto.request.CreateAbsenceRequest;
import com.newwork.employee.dto.request.UpdateAbsenceStatusRequest;
import com.newwork.employee.entity.AbsenceRequest;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.AbsenceStatus;
import com.newwork.employee.entity.enums.AbsenceType;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.repository.AbsenceRequestRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.impl.AbsenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceImplTest {

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AbsenceServiceImpl absenceService;

    private User manager;
    private User employee;

    @BeforeEach
    void setup() {
        manager = User.builder()
                .id(UUID.randomUUID())
                .email("manager@test.com")
                .employeeId("MGR-001")
                .role(Role.MANAGER)
                .build();

        employee = User.builder()
                .id(UUID.randomUUID())
                .email("emp@test.com")
                .employeeId("EMP-001")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
    }

    @Test
    void submitShouldPersistRequest() {
        CreateAbsenceRequest dto = new CreateAbsenceRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                AbsenceType.VACATION,
                "Vacation"
        );
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(absenceRequestRepository.save(any(AbsenceRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = absenceService.submit(employee.getId(), dto);

        assertThat(result.getUserId()).isEqualTo(employee.getId());
        assertThat(result.getManagerId()).isEqualTo(manager.getId());
        assertThat(result.getStatus()).isEqualTo(AbsenceStatus.PENDING);
    }

    @Test
    void updateStatusShouldApprove() {
        AbsenceRequest request = AbsenceRequest.builder()
                .id(UUID.randomUUID())
                .user(employee)
                .manager(manager)
                .status(AbsenceStatus.PENDING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .type(AbsenceType.SICK)
                .build();
        when(absenceRequestRepository.findByIdWithUserAndManager(request.getId()))
                .thenReturn(Optional.of(request));
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = absenceService.updateStatus(manager.getId(), request.getId(),
                UpdateAbsenceStatusRequest.builder().action("APPROVE").build());

        assertThat(result.getStatus()).isEqualTo(AbsenceStatus.APPROVED);
    }

    @Test
    void updateStatusShouldRejectNonManager() {
        AbsenceRequest request = AbsenceRequest.builder()
                .id(UUID.randomUUID())
                .user(employee)
                .manager(manager)
                .status(AbsenceStatus.PENDING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .type(AbsenceType.SICK)
                .build();
        when(absenceRequestRepository.findByIdWithUserAndManager(request.getId()))
                .thenReturn(Optional.of(request));

        assertThatThrownBy(() -> absenceService.updateStatus(UUID.randomUUID(), request.getId(),
                        UpdateAbsenceStatusRequest.builder().action("APPROVE").build()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatusShouldRejectWithNote() {
        AbsenceRequest request = AbsenceRequest.builder()
                .id(UUID.randomUUID())
                .user(employee)
                .manager(manager)
                .status(AbsenceStatus.PENDING)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .type(AbsenceType.SICK)
                .build();
        when(absenceRequestRepository.findByIdWithUserAndManager(request.getId()))
                .thenReturn(Optional.of(request));
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = absenceService.updateStatus(manager.getId(), request.getId(),
                UpdateAbsenceStatusRequest.builder().action("REJECT").note("Need coverage").build());

        assertThat(result.getStatus()).isEqualTo(AbsenceStatus.REJECTED);
        assertThat(result.getNote()).isEqualTo("Need coverage");
    }

    @Test
    void submitShouldValidateDates() {
        CreateAbsenceRequest dto = new CreateAbsenceRequest(
                LocalDate.now(),
                LocalDate.now().minusDays(1),
                AbsenceType.PERSONAL,
                "Invalid dates"
        );

        assertThatThrownBy(() -> absenceService.submit(employee.getId(), dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitShouldFailWhenUserMissing() {
        when(userRepository.findById(employee.getId())).thenReturn(Optional.empty());
        CreateAbsenceRequest dto = new CreateAbsenceRequest(
                LocalDate.now(),
                LocalDate.now(),
                AbsenceType.VACATION,
                null
        );

        assertThatThrownBy(() -> absenceService.submit(employee.getId(), dto))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
