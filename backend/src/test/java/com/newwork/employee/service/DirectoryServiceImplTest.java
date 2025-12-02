package com.newwork.employee.service;

import com.newwork.employee.dto.CoworkerDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.entity.enums.WorkLocationType;
import com.newwork.employee.repository.EmployeeAbsenceRepository;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.impl.DirectoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectoryServiceImplTest {

    @Mock
    private EmployeeProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private EmployeeAbsenceRepository absenceRequestRepository;

    @InjectMocks
    private DirectoryServiceImpl directoryService;

    private User viewer;
    private User directReportUser;
    private User coworkerUser;
    private EmployeeProfile directReportProfile;
    private EmployeeProfile coworkerProfile;

    @BeforeEach
    void setUp() {
        viewer = User.builder()
                .id(UUID.randomUUID())
                .employeeId("MGR-001")
                .email("manager@test.com")
                .build();

        directReportUser = User.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP-010")
                .email("emp10@test.com")
                .manager(viewer)
                .build();

        coworkerUser = User.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP-020")
                .email("emp20@test.com")
                .manager(null)
                .build();

        directReportProfile = EmployeeProfile.builder()
                .user(directReportUser)
                .legalFirstName("Alice")
                .legalLastName("Anderson")
                .preferredName("Alice A.")
                .jobTitle("Senior Engineer")
                .department("Engineering")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2020, 1, 1))
                .workLocationType(WorkLocationType.REMOTE)
                .build();

        coworkerProfile = EmployeeProfile.builder()
                .user(coworkerUser)
                .legalFirstName("Bob")
                .legalLastName("Brown")
                .preferredName("Bob B.")
                .jobTitle("Product Manager")
                .department("Product")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2019, 5, 1))
                .workLocationType(WorkLocationType.HYBRID)
                .build();
    }

    @Test
    void shouldReturnDirectoryWithRelationshipsAndSorting() {
        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(profileRepository.findAllActiveProfilesWithUserAndManager())
                .thenReturn(List.of(directReportProfile, coworkerProfile));
        when(permissionService.determineRelationship(viewer, directReportUser))
                .thenReturn(Relationship.MANAGER);
        when(permissionService.determineRelationship(viewer, coworkerUser))
                .thenReturn(Relationship.COWORKER);
        when(absenceRequestRepository.countByManagerAndUserAndStatus(any(), any(), any()))
                .thenReturn(0L);

        List<CoworkerDTO> result = directoryService.getDirectory(viewer.getId(), null, null, null);

        assertThat(result).hasSize(2);
        CoworkerDTO first = result.get(0);
        CoworkerDTO second = result.get(1);

        // Alphabetical by preferred name
        assertThat(first.getPreferredName()).isEqualTo("Alice A.");
        assertThat(first.isDirectReport()).isTrue();
        assertThat(first.getRelationship()).isEqualTo("MANAGER");

        assertThat(second.getPreferredName()).isEqualTo("Bob B.");
        assertThat(second.isDirectReport()).isFalse();
        assertThat(second.getRelationship()).isEqualTo("OTHER");
    }

    @Test
    void shouldFilterBySearchAndDepartment() {
        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(profileRepository.findAllActiveProfilesWithUserAndManager())
                .thenReturn(List.of(directReportProfile, coworkerProfile));
        when(permissionService.determineRelationship(any(User.class), any(User.class)))
                .thenReturn(Relationship.COWORKER);

        List<CoworkerDTO> result = directoryService.getDirectory(
                viewer.getId(), "prod", "product", null);

        assertThat(result)
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.getEmployeeId()).isEqualTo("EMP-020");
                    assertThat(dto.getDepartment()).isEqualTo("Product");
                });
    }

    @Test
    void shouldReturnOnlyDirectReportsWhenRequested() {
        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));
        when(profileRepository.findAllActiveProfilesWithUserAndManager())
                .thenReturn(List.of(directReportProfile, coworkerProfile));
        when(permissionService.determineRelationship(viewer, directReportUser))
                .thenReturn(Relationship.MANAGER);
        when(permissionService.determineRelationship(viewer, coworkerUser))
                .thenReturn(Relationship.COWORKER);
        when(absenceRequestRepository.countByManagerAndUserAndStatus(any(), any(), any()))
                .thenReturn(2L);

        List<CoworkerDTO> result = directoryService.getDirectory(viewer.getId(), null, null, true);

        assertThat(result)
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.isDirectReport()).isTrue();
                    assertThat(dto.getPendingAbsenceCount()).isEqualTo(2);
                });
    }
}
