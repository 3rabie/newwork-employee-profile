package com.newwork.employee.testutil;

import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.Role;

import java.util.UUID;

/**
 * Test builder for creating User test data with sensible defaults.
 * Provides a fluent API for customizing test users.
 */
public class UserTestBuilder {

    private UUID id = UUID.randomUUID();
    private String employeeId = "EMP-" + System.currentTimeMillis();
    private String email = "test.user@example.com";
    private String password = "encodedPassword123";
    private Role role = Role.EMPLOYEE;
    private User manager;

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public static UserTestBuilder anEmployee() {
        return new UserTestBuilder().withRole(Role.EMPLOYEE);
    }

    public static UserTestBuilder aManager() {
        return new UserTestBuilder().withRole(Role.MANAGER);
    }

    public UserTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withEmployeeId(String employeeId) {
        this.employeeId = employeeId;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public UserTestBuilder withManager(User manager) {
        this.manager = manager;
        return this;
    }

    public User build() {
        return User.builder()
                .id(id)
                .employeeId(employeeId)
                .email(email)
                .password(password)
                .role(role)
                .manager(manager)
                .build();
    }
}
