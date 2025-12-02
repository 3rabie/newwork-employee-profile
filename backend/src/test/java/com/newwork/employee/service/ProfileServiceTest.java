package com.newwork.employee.service;

import com.newwork.employee.dto.ProfileDTO;
import com.newwork.employee.dto.ProfileUpdateDTO;
import com.newwork.employee.entity.EmployeeProfile;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.EmploymentStatus;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.entity.enums.WorkLocationType;
import com.newwork.employee.exception.ForbiddenException;
import com.newwork.employee.exception.ResourceNotFoundException;
import com.newwork.employee.mapper.ProfileMapper;
import com.newwork.employee.repository.EmployeeProfileRepository;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Tests")
class ProfileServiceTest {

    @Mock
    private EmployeeProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private UUID viewerId;
    private UUID profileOwnerId;
    private User profileOwner;
    private EmployeeProfile profile;
    private ProfileDTO profileDTO;

    @BeforeEach
    void setUp() {
        viewerId = UUID.randomUUID();
        profileOwnerId = UUID.randomUUID();

        profileOwner = User.builder()
                .id(profileOwnerId)
                .employeeId("EMP-001")
                .email("emp1@test.com")
                .password("password")
                .role(Role.EMPLOYEE)
                .build();

        profile = EmployeeProfile.builder()
                .id(UUID.randomUUID())
                .user(profileOwner)
                .legalFirstName("John")
                .legalLastName("Doe")
                .department("Engineering")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .hireDate(LocalDate.of(2020, 1, 1))
                .fte(new BigDecimal("1.00"))
                .preferredName("Johnny")
                .jobTitle("Software Engineer")
                .bio("Test bio")
                .personalEmail("john.personal@test.com")
                .salary(new BigDecimal("90000"))
                .build();

        profileDTO = ProfileDTO.builder()
                .id(profile.getId())
                .userId(profileOwnerId)
                .legalFirstName("John")
                .legalLastName("Doe")
                .preferredName("Johnny")
                .build();
    }

    @Nested
    @DisplayName("Get Profile")
    class GetProfileTests {

        @Test
        @DisplayName("Should get profile with SELF relationship")
        void shouldGetProfileWithSelfRelationship() {
            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(profileOwnerId, profileOwnerId))
                    .thenReturn(Relationship.SELF);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.getProfile(profileOwnerId, profileOwnerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(profile.getId());
            verify(profileRepository).findByUserId(profileOwnerId);
            verify(permissionService).determineRelationship(profileOwnerId, profileOwnerId);
            verify(profileMapper).toDTO(eq(profile), anySet());
        }

        @Test
        @DisplayName("Should get profile with MANAGER relationship")
        void shouldGetProfileWithManagerRelationship() {
            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(viewerId, profileOwnerId))
                    .thenReturn(Relationship.MANAGER);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.getProfile(viewerId, profileOwnerId);

            assertThat(result).isNotNull();
            verify(permissionService).determineRelationship(viewerId, profileOwnerId);
            verify(profileMapper).toDTO(eq(profile), anySet());
        }

        @Test
        @DisplayName("Should get profile with COWORKER relationship")
        void shouldGetProfileWithCoworkerRelationship() {
            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(viewerId, profileOwnerId))
                    .thenReturn(Relationship.COWORKER);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.getProfile(viewerId, profileOwnerId);

            assertThat(result).isNotNull();
            verify(profileMapper).toDTO(eq(profile), anySet());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when profile not found")
        void shouldThrowExceptionWhenProfileNotFound() {
            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(viewerId, profileOwnerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Profile not found");
        }
    }

    @Nested
    @DisplayName("Update Profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update non-sensitive fields as SELF")
        void shouldUpdateNonSensitiveFieldsAsSelf() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .preferredName("Updated Name")
                    .bio("Updated bio")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(profileOwnerId, profileOwnerId))
                    .thenReturn(Relationship.SELF);
            when(permissionService.canEdit(Relationship.SELF, FieldType.NON_SENSITIVE)).thenReturn(true);
            when(profileRepository.save(any(EmployeeProfile.class))).thenReturn(profile);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.updateProfile(profileOwnerId, profileOwnerId, updateDTO);

            assertThat(result).isNotNull();
            verify(profileRepository).save(any(EmployeeProfile.class));
            assertThat(profile.getPreferredName()).isEqualTo("Updated Name");
            assertThat(profile.getBio()).isEqualTo("Updated bio");
        }

        @Test
        @DisplayName("Should update sensitive fields as SELF")
        void shouldUpdateSensitiveFieldsAsSelf() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .personalEmail("new.email@test.com")
                    .personalPhone("+1-555-9999")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(profileOwnerId, profileOwnerId))
                    .thenReturn(Relationship.SELF);
            when(permissionService.canEdit(Relationship.SELF, FieldType.SENSITIVE)).thenReturn(true);
            when(profileRepository.save(any(EmployeeProfile.class))).thenReturn(profile);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.updateProfile(profileOwnerId, profileOwnerId, updateDTO);

            assertThat(result).isNotNull();
            verify(profileRepository).save(any(EmployeeProfile.class));
            assertThat(profile.getPersonalEmail()).isEqualTo("new.email@test.com");
            assertThat(profile.getPersonalPhone()).isEqualTo("+1-555-9999");
        }

        @Test
        @DisplayName("Should update non-sensitive fields as MANAGER for direct report")
        void shouldUpdateNonSensitiveFieldsAsManager() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .jobTitle("Senior Software Engineer")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(viewerId, profileOwnerId))
                    .thenReturn(Relationship.MANAGER);
            when(permissionService.canEdit(Relationship.MANAGER, FieldType.NON_SENSITIVE)).thenReturn(true);
            when(profileRepository.save(any(EmployeeProfile.class))).thenReturn(profile);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.updateProfile(viewerId, profileOwnerId, updateDTO);

            assertThat(result).isNotNull();
            verify(profileRepository).save(any(EmployeeProfile.class));
            assertThat(profile.getJobTitle()).isEqualTo("Senior Software Engineer");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when MANAGER tries to edit sensitive fields")
        void shouldThrowExceptionWhenManagerTriesToEditSensitiveFields() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .personalEmail("manager.trying@test.com")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(viewerId, profileOwnerId))
                    .thenReturn(Relationship.MANAGER);
            when(permissionService.canEdit(Relationship.MANAGER, FieldType.SENSITIVE)).thenReturn(false);

            assertThatThrownBy(() -> profileService.updateProfile(viewerId, profileOwnerId, updateDTO))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("sensitive fields");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when COWORKER tries to edit non-sensitive fields")
        void shouldThrowExceptionWhenCoworkerTriesToEditNonSensitiveFields() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .preferredName("Hacker Name")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(viewerId, profileOwnerId))
                    .thenReturn(Relationship.COWORKER);
            when(permissionService.canEdit(Relationship.COWORKER, FieldType.NON_SENSITIVE)).thenReturn(false);

            assertThatThrownBy(() -> profileService.updateProfile(viewerId, profileOwnerId, updateDTO))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("non-sensitive fields");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when COWORKER tries to edit sensitive fields")
        void shouldThrowExceptionWhenCoworkerTriesToEditSensitiveFields() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .personalEmail("coworker.trying@test.com")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(viewerId, profileOwnerId))
                    .thenReturn(Relationship.COWORKER);
            when(permissionService.canEdit(Relationship.COWORKER, FieldType.SENSITIVE)).thenReturn(false);

            assertThatThrownBy(() -> profileService.updateProfile(viewerId, profileOwnerId, updateDTO))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("sensitive fields");
        }

        @Test
        @DisplayName("Should update multiple fields at once")
        void shouldUpdateMultipleFieldsAtOnce() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .preferredName("Updated Name")
                    .jobTitle("Senior Engineer")
                    .bio("Updated bio")
                    .workLocationType(WorkLocationType.REMOTE)
                    .personalEmail("new@test.com")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.of(profile));
            when(permissionService.determineRelationship(profileOwnerId, profileOwnerId))
                    .thenReturn(Relationship.SELF);
            when(permissionService.canEdit(Relationship.SELF, FieldType.NON_SENSITIVE)).thenReturn(true);
            when(permissionService.canEdit(Relationship.SELF, FieldType.SENSITIVE)).thenReturn(true);
            when(profileRepository.save(any(EmployeeProfile.class))).thenReturn(profile);
            when(permissionService.canView(any(Relationship.class), any(FieldType.class))).thenReturn(true);
            when(profileMapper.toDTO(eq(profile), anySet())).thenReturn(profileDTO);

            ProfileDTO result = profileService.updateProfile(profileOwnerId, profileOwnerId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(profile.getPreferredName()).isEqualTo("Updated Name");
            assertThat(profile.getJobTitle()).isEqualTo("Senior Engineer");
            assertThat(profile.getBio()).isEqualTo("Updated bio");
            assertThat(profile.getWorkLocationType()).isEqualTo(WorkLocationType.REMOTE);
            assertThat(profile.getPersonalEmail()).isEqualTo("new@test.com");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent profile")
        void shouldThrowExceptionWhenUpdatingNonExistentProfile() {
            ProfileUpdateDTO updateDTO = ProfileUpdateDTO.builder()
                    .preferredName("Test")
                    .build();

            when(profileRepository.findByUserId(profileOwnerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(viewerId, profileOwnerId, updateDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Profile not found");
        }
    }
}
