package com.newwork.employee.mapper;

import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper utility for converting User entity to AuthResponse DTO.
 * Handles mapping logic for authentication responses.
 */
@Component
public class AuthMapper {

    /**
     * Build AuthResponse from User entity and JWT token
     *
     * @param user the authenticated user
     * @param token the generated JWT token
     * @return AuthResponse containing user details and token
     */
    public AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .role(user.getRole().name())
                .managerId(user.getManager() != null ? user.getManager().getId() : null)
                .build();
    }
}
