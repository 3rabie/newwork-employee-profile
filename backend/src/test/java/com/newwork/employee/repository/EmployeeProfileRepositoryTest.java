package com.newwork.employee.repository;

import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.entity.enums.WorkLocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("EmployeeProfileRepository Tests")
class EmployeeProfileRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    private User manager;
    private User employee1;
    private User employee2;
    private EmployeeProfile managerProfile;
    private EmployeeProfile employee1Profile;
    private EmployeeProfile employee2Profile;

    @BeforeEach
    void setUp() {
        // Clear all data
        profileRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create manager
        manager = User.builder()
                .employeeId("MGR-001")
                .email("manager@test.com")
                .password("hashedPassword")
                .role(Role.MANAGER)
                .manager(null)
                .build();
        manager = userRepository.save(manager);

        // Create employees
        employee1 = User.builder()
                .employeeId("EMP-001")
                .email("emp1@test.com")
                .password("hashedPassword")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
        employee1 = userRepository.save(employee1);

        employee2 = User.builder()
                .employeeId("EMP-002")
                .email("emp2@test.com")
                .password("hashedPassword")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
        employee2 = userRepository.save(employee2);

        // Create profiles
        managerProfile = createProfile(manager, "Engineering", EmploymentStatus.ACTIVE);
        employee1Profile = createProfile(employee1, "Engineering", EmploymentStatus.ACTIVE);
        employee2Profile = createProfile(employee2, "Sales", EmploymentStatus.ON_LEAVE);

        entityManager.flush();
        entityManager.clear();
    }

    private EmployeeProfile createProfile(User user, String department, EmploymentStatus status) {
        return profileRepository.save(EmployeeProfile.builder()
                .user(user)
                .legalFirstName("First_" + user.getEmployeeId())
                .legalLastName("Last_" + user.getEmployeeId())
                .department(department)
                .jobCode("JOB-" + user.getEmployeeId())
                .jobFamily(department)
                .jobLevel("Senior")
                .employmentStatus(status)
                .hireDate(LocalDate.of(2020, 1, 1))
                .fte(new BigDecimal("1.00"))
                .preferredName("Preferred_" + user.getEmployeeId())
                .jobTitle("Software Engineer")
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

    @Test
    @DisplayName("Should save and retrieve employee profile")
    void shouldSaveAndRetrieveProfile() {
        // Given - profiles created in setUp

        // When
        Optional<EmployeeProfile> found = profileRepository.findById(employee1Profile.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getLegalFirstName()).isEqualTo("First_EMP-001");
        assertThat(found.get().getLegalLastName()).isEqualTo("Last_EMP-001");
        assertThat(found.get().getDepartment()).isEqualTo("Engineering");
        assertThat(found.get().getEmploymentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find profile by user ID")
    void shouldFindProfileByUserId() {
        // When
        Optional<EmployeeProfile> found = profileRepository.findByUserId(employee1.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(employee1.getId());
        assertThat(found.get().getUser().getEmail()).isEqualTo("emp1@test.com");
    }

    @Test
    @DisplayName("Should return empty when profile not found by user ID")
    void shouldReturnEmptyWhenProfileNotFoundByUserId() {
        // Given
        User userWithoutProfile = User.builder()
                .employeeId("NO-PROFILE")
                .email("noprofile@test.com")
                .password("password")
                .role(Role.EMPLOYEE)
                .build();
        userWithoutProfile = userRepository.save(userWithoutProfile);
        entityManager.flush();

        // When
        Optional<EmployeeProfile> found = profileRepository.findByUserId(userWithoutProfile.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find profiles by employment status")
    void shouldFindProfilesByEmploymentStatus() {
        // When
        List<EmployeeProfile> activeProfiles = profileRepository.findByEmploymentStatus(EmploymentStatus.ACTIVE);
        List<EmployeeProfile> onLeaveProfiles = profileRepository.findByEmploymentStatus(EmploymentStatus.ON_LEAVE);

        // Then
        assertThat(activeProfiles).hasSize(2);
        assertThat(activeProfiles).extracting(EmployeeProfile::getEmploymentStatus)
                .containsOnly(EmploymentStatus.ACTIVE);

        assertThat(onLeaveProfiles).hasSize(1);
        assertThat(onLeaveProfiles.get(0).getUser().getId()).isEqualTo(employee2.getId());
    }

    @Test
    @DisplayName("Should find all active profiles")
    void shouldFindAllActiveProfiles() {
        // When
        List<EmployeeProfile> activeProfiles = profileRepository.findAllActiveProfiles();

        // Then
        assertThat(activeProfiles).hasSize(2);
        assertThat(activeProfiles).extracting(p -> p.getUser().getEmployeeId())
                .containsExactlyInAnyOrder("MGR-001", "EMP-001");
    }

    @Test
    @DisplayName("Should find profiles by department")
    void shouldFindProfilesByDepartment() {
        // When
        List<EmployeeProfile> engineeringProfiles = profileRepository.findByDepartment("Engineering");
        List<EmployeeProfile> salesProfiles = profileRepository.findByDepartment("Sales");

        // Then
        assertThat(engineeringProfiles).hasSize(2);
        assertThat(engineeringProfiles).extracting(p -> p.getUser().getEmployeeId())
                .containsExactlyInAnyOrder("MGR-001", "EMP-001");

        assertThat(salesProfiles).hasSize(1);
        assertThat(salesProfiles.get(0).getUser().getEmployeeId()).isEqualTo("EMP-002");
    }

    @Test
    @DisplayName("Should check if profile exists by user ID")
    void shouldCheckIfProfileExistsByUserId() {
        // When
        boolean exists = profileRepository.existsByUserId(employee1.getId());
        boolean notExists = profileRepository.existsByUserId(java.util.UUID.randomUUID());

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find profiles by manager ID (direct reports)")
    void shouldFindProfilesByManagerId() {
        // When
        List<EmployeeProfile> directReports = profileRepository.findByManagerId(manager.getId());

        // Then
        assertThat(directReports).hasSize(2);
        assertThat(directReports).extracting(p -> p.getUser().getEmployeeId())
                .containsExactlyInAnyOrder("EMP-001", "EMP-002");
        assertThat(directReports).allMatch(p -> p.getUser().getManager().getId().equals(manager.getId()));
    }

    @Test
    @DisplayName("Should return empty list when manager has no direct reports")
    void shouldReturnEmptyListWhenManagerHasNoDirectReports() {
        // When - employee1 is not a manager, so no direct reports
        List<EmployeeProfile> directReports = profileRepository.findByManagerId(employee1.getId());

        // Then
        assertThat(directReports).isEmpty();
    }

    @Test
    @DisplayName("Should update profile fields")
    void shouldUpdateProfileFields() {
        // Given
        EmployeeProfile profile = profileRepository.findByUserId(employee1.getId()).get();

        // When
        profile.setPreferredName("NewPreferredName");
        profile.setBio("Updated bio");
        profile.setWorkLocationType(WorkLocationType.REMOTE);
        EmployeeProfile updated = profileRepository.save(profile);
        entityManager.flush();
        entityManager.clear();

        // Then
        EmployeeProfile retrieved = profileRepository.findById(profile.getId()).get();
        assertThat(retrieved.getPreferredName()).isEqualTo("NewPreferredName");
        assertThat(retrieved.getBio()).isEqualTo("Updated bio");
        assertThat(retrieved.getWorkLocationType()).isEqualTo(WorkLocationType.REMOTE);
    }

    @Test
    @DisplayName("Should delete profile by ID")
    void shouldDeleteProfileById() {
        // Given
        Long countBefore = profileRepository.count();

        // When
        profileRepository.deleteById(employee1Profile.getId());
        entityManager.flush();

        // Then
        assertThat(profileRepository.count()).isEqualTo(countBefore - 1);
        assertThat(profileRepository.findById(employee1Profile.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should cascade delete profile when user is deleted")
    void shouldCascadeDeleteProfileWhenUserDeleted() {
        // Given
        Long profileCountBefore = profileRepository.count();

        // When
        userRepository.deleteById(employee1.getId());
        entityManager.flush();

        // Then
        assertThat(profileRepository.count()).isEqualTo(profileCountBefore - 1);
        assertThat(profileRepository.findByUserId(employee1.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique constraint on user_id")
    void shouldEnforceUniqueConstraintOnUserId() {
        // Given
        EmployeeProfile duplicateProfile = EmployeeProfile.builder()
                .user(employee1) // Same user as employee1Profile
                .legalFirstName("Duplicate")
                .legalLastName("Profile")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .fte(new BigDecimal("1.00"))
                .build();

        // When/Then
        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    profileRepository.save(duplicateProfile);
                    entityManager.flush();
                }
        );
        assertThat(exception.getMessage().toLowerCase())
                .containsAnyOf("unique constraint", "duplicate key", "unique");
    }

    @Test
    @DisplayName("Should validate FTE constraint")
    void shouldValidateFteConstraint() {
        // Given
        EmployeeProfile invalidProfile = EmployeeProfile.builder()
                .user(userRepository.save(User.builder()
                        .employeeId("TEST-FTE")
                        .email("ftetest@test.com")
                        .password("password")
                        .role(Role.EMPLOYEE)
                        .build()))
                .legalFirstName("Test")
                .legalLastName("FTE")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .fte(new BigDecimal("1.50")) // Invalid: > 1.00
                .build();

        // When/Then
        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    profileRepository.save(invalidProfile);
                    entityManager.flush();
                }
        );
        assertThat(exception.getMessage().toLowerCase())
                .containsAnyOf("chk_fte", "constraint", "check");
    }

    @Test
    @DisplayName("Should store all field types correctly")
    void shouldStoreAllFieldTypesCorrectly() {
        // When
        EmployeeProfile profile = profileRepository.findByUserId(employee1.getId()).get();

        // Then - System-Managed Fields
        assertThat(profile.getLegalFirstName()).isNotNull();
        assertThat(profile.getLegalLastName()).isNotNull();
        assertThat(profile.getDepartment()).isNotNull();
        assertThat(profile.getJobCode()).isNotNull();
        assertThat(profile.getEmploymentStatus()).isNotNull();
        assertThat(profile.getHireDate()).isNotNull();
        assertThat(profile.getFte()).isEqualTo(new BigDecimal("1.00"));

        // Then - Non-Sensitive Fields
        assertThat(profile.getPreferredName()).isNotNull();
        assertThat(profile.getJobTitle()).isNotNull();
        assertThat(profile.getOfficeLocation()).isNotNull();
        assertThat(profile.getWorkPhone()).isNotNull();
        assertThat(profile.getWorkLocationType()).isNotNull();
        assertThat(profile.getBio()).isNotNull();
        assertThat(profile.getSkills()).isNotNull();

        // Then - Sensitive Fields
        assertThat(profile.getPersonalEmail()).isNotNull();
        assertThat(profile.getPersonalPhone()).isNotNull();
        assertThat(profile.getDateOfBirth()).isNotNull();
        assertThat(profile.getAbsenceBalanceDays()).isNotNull();
        assertThat(profile.getSalary()).isNotNull();
        assertThat(profile.getPerformanceRating()).isNotNull();

        // Then - Audit Fields
        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getUpdatedAt()).isNotNull();
    }
}
