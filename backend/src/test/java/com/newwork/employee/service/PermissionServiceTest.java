package com.newwork.employee.service;

import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.FieldType;
import com.newwork.employee.entity.enums.Relationship;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.exception.UserNotFoundException;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Tests")
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private User employee;
    private User manager;
    private User coworker;

    @BeforeEach
    void setUp() {
        // Setup manager
        manager = User.builder()
                .id(UUID.randomUUID())
                .employeeId("100")
                .email("manager@company.com")
                .password("hashedPassword")
                .role(Role.MANAGER)
                .manager(null)
                .build();

        // Setup employee (managed by manager)
        employee = User.builder()
                .id(UUID.randomUUID())
                .employeeId("101")
                .email("employee@company.com")
                .password("hashedPassword")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();

        // Setup coworker (also managed by manager, but not the profile being viewed)
        coworker = User.builder()
                .id(UUID.randomUUID())
                .employeeId("102")
                .email("coworker@company.com")
                .password("hashedPassword")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
    }

    @Nested
    @DisplayName("Relationship Detection Tests")
    class RelationshipDetectionTests {

        @Test
        @DisplayName("Should detect SELF relationship when viewing own profile")
        void shouldDetectSelfRelationship() {
            // When viewing own profile using UUID
            Relationship result1 = permissionService.determineRelationship(employee.getId(), employee.getId());
            assertThat(result1).isEqualTo(Relationship.SELF);

            // When viewing own profile using User objects
            Relationship result2 = permissionService.determineRelationship(employee, employee);
            assertThat(result2).isEqualTo(Relationship.SELF);
        }

        @Test
        @DisplayName("Should detect MANAGER relationship when manager views direct report")
        void shouldDetectManagerRelationship() {
            // Mock repository call
            when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // When manager views employee profile using UUID
            Relationship result1 = permissionService.determineRelationship(manager.getId(), employee.getId());
            assertThat(result1).isEqualTo(Relationship.MANAGER);

            // When manager views employee profile using User objects
            Relationship result2 = permissionService.determineRelationship(manager, employee);
            assertThat(result2).isEqualTo(Relationship.MANAGER);
        }

        @Test
        @DisplayName("Should detect COWORKER relationship when viewing peer profile")
        void shouldDetectCoworkerRelationship() {
            // Mock repository call
            when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // When coworker views employee profile using UUID
            Relationship result1 = permissionService.determineRelationship(coworker.getId(), employee.getId());
            assertThat(result1).isEqualTo(Relationship.COWORKER);

            // When coworker views employee profile using User objects
            Relationship result2 = permissionService.determineRelationship(coworker, employee);
            assertThat(result2).isEqualTo(Relationship.COWORKER);
        }

        @Test
        @DisplayName("Should detect COWORKER when employee views manager profile")
        void shouldDetectCoworkerWhenEmployeeViewsManager() {
            // Mock repository call
            when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

            // When employee views manager profile
            Relationship result = permissionService.determineRelationship(employee.getId(), manager.getId());
            assertThat(result).isEqualTo(Relationship.COWORKER);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when profile owner not found")
        void shouldThrowExceptionWhenProfileOwnerNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    permissionService.determineRelationship(employee.getId(), unknownId)
            )
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("Profile owner not found");
        }

        @Test
        @DisplayName("Should detect COWORKER when profile owner has no manager")
        void shouldDetectCoworkerWhenProfileOwnerHasNoManager() {
            // Manager has no manager
            when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

            Relationship result = permissionService.determineRelationship(employee.getId(), manager.getId());
            assertThat(result).isEqualTo(Relationship.COWORKER);
        }
    }

    @Nested
    @DisplayName("View Permission Tests")
    class ViewPermissionTests {

        @Test
        @DisplayName("SELF can view all field types")
        void selfCanViewAllFieldTypes() {
            assertThat(permissionService.canView(Relationship.SELF, FieldType.SYSTEM_MANAGED)).isTrue();
            assertThat(permissionService.canView(Relationship.SELF, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canView(Relationship.SELF, FieldType.SENSITIVE)).isTrue();
        }

        @Test
        @DisplayName("MANAGER can view all field types")
        void managerCanViewAllFieldTypes() {
            assertThat(permissionService.canView(Relationship.MANAGER, FieldType.SYSTEM_MANAGED)).isTrue();
            assertThat(permissionService.canView(Relationship.MANAGER, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canView(Relationship.MANAGER, FieldType.SENSITIVE)).isTrue();
        }

        @Test
        @DisplayName("COWORKER can view only SYSTEM_MANAGED and NON_SENSITIVE fields")
        void coworkerCanViewLimitedFields() {
            assertThat(permissionService.canView(Relationship.COWORKER, FieldType.SYSTEM_MANAGED)).isTrue();
            assertThat(permissionService.canView(Relationship.COWORKER, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canView(Relationship.COWORKER, FieldType.SENSITIVE)).isFalse();
        }

        @Test
        @DisplayName("canView with UUID should work correctly for SELF")
        void canViewWithUuidForSelf() {
            assertThat(permissionService.canView(employee.getId(), employee.getId(), FieldType.SENSITIVE)).isTrue();
        }

        @Test
        @DisplayName("canView with UUID should work correctly for MANAGER")
        void canViewWithUuidForManager() {
            when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThat(permissionService.canView(manager.getId(), employee.getId(), FieldType.SENSITIVE)).isTrue();
        }

        @Test
        @DisplayName("canView with UUID should work correctly for COWORKER")
        void canViewWithUuidForCoworker() {
            when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThat(permissionService.canView(coworker.getId(), employee.getId(), FieldType.SENSITIVE)).isFalse();
        }
    }

    @Nested
    @DisplayName("Edit Permission Tests")
    class EditPermissionTests {

        @Test
        @DisplayName("No one can edit SYSTEM_MANAGED fields")
        void noOneCanEditSystemManagedFields() {
            assertThat(permissionService.canEdit(Relationship.SELF, FieldType.SYSTEM_MANAGED)).isFalse();
            assertThat(permissionService.canEdit(Relationship.MANAGER, FieldType.SYSTEM_MANAGED)).isFalse();
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.SYSTEM_MANAGED)).isFalse();
        }

        @Test
        @DisplayName("SELF can edit NON_SENSITIVE and SENSITIVE fields")
        void selfCanEditPersonalFields() {
            assertThat(permissionService.canEdit(Relationship.SELF, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.SELF, FieldType.SENSITIVE)).isTrue();
        }

        @Test
        @DisplayName("MANAGER can edit NON_SENSITIVE but not SENSITIVE fields")
        void managerCanEditLimitedFields() {
            assertThat(permissionService.canEdit(Relationship.MANAGER, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.MANAGER, FieldType.SENSITIVE)).isFalse();
        }

        @Test
        @DisplayName("COWORKER cannot edit any fields")
        void coworkerCannotEditAnyFields() {
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.SYSTEM_MANAGED)).isFalse();
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.NON_SENSITIVE)).isFalse();
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.SENSITIVE)).isFalse();
        }

        @Test
        @DisplayName("canEdit with UUID should work correctly for SELF")
        void canEditWithUuidForSelf() {
            assertThat(permissionService.canEdit(employee.getId(), employee.getId(), FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(employee.getId(), employee.getId(), FieldType.SENSITIVE)).isTrue();
        }

        @Test
        @DisplayName("canEdit with UUID should work correctly for MANAGER")
        void canEditWithUuidForManager() {
            when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThat(permissionService.canEdit(manager.getId(), employee.getId(), FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(manager.getId(), employee.getId(), FieldType.SENSITIVE)).isFalse();
        }

        @Test
        @DisplayName("canEdit with UUID should work correctly for COWORKER")
        void canEditWithUuidForCoworker() {
            when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            assertThat(permissionService.canEdit(coworker.getId(), employee.getId(), FieldType.NON_SENSITIVE)).isFalse();
        }
    }

    @Nested
    @DisplayName("Permission Matrix Verification")
    class PermissionMatrixTests {

        @Test
        @DisplayName("Verify complete permission matrix matches PRD")
        void verifyCompletePermissionMatrix() {
            // SELF permissions
            assertThat(permissionService.canView(Relationship.SELF, FieldType.SYSTEM_MANAGED)).isTrue();
            assertThat(permissionService.canEdit(Relationship.SELF, FieldType.SYSTEM_MANAGED)).isFalse();
            assertThat(permissionService.canView(Relationship.SELF, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.SELF, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canView(Relationship.SELF, FieldType.SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.SELF, FieldType.SENSITIVE)).isTrue();

            // MANAGER permissions
            assertThat(permissionService.canView(Relationship.MANAGER, FieldType.SYSTEM_MANAGED)).isTrue();
            assertThat(permissionService.canEdit(Relationship.MANAGER, FieldType.SYSTEM_MANAGED)).isFalse();
            assertThat(permissionService.canView(Relationship.MANAGER, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.MANAGER, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canView(Relationship.MANAGER, FieldType.SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.MANAGER, FieldType.SENSITIVE)).isFalse();

            // COWORKER permissions
            assertThat(permissionService.canView(Relationship.COWORKER, FieldType.SYSTEM_MANAGED)).isTrue();
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.SYSTEM_MANAGED)).isFalse();
            assertThat(permissionService.canView(Relationship.COWORKER, FieldType.NON_SENSITIVE)).isTrue();
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.NON_SENSITIVE)).isFalse();
            assertThat(permissionService.canView(Relationship.COWORKER, FieldType.SENSITIVE)).isFalse();
            assertThat(permissionService.canEdit(Relationship.COWORKER, FieldType.SENSITIVE)).isFalse();
        }
    }
}
