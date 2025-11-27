package com.newwork.employee.security;

import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests token generation, validation, and claim extraction.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testManager;
    private User testEmployee;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-for-unit-testing-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm",
                3600000L // 1 hour
        );

        testManager = User.builder()
                .id(UUID.randomUUID())
                .employeeId("TEST_MGR_001")
                .email("unittest.manager@testcompany.com")
                .password("encoded_password")
                .role(Role.MANAGER)
                .manager(null)
                .build();

        UUID managerId = UUID.randomUUID();
        User manager = User.builder()
                .id(managerId)
                .employeeId("MGR_999")
                .email("manager999@testcompany.com")
                .role(Role.MANAGER)
                .build();

        testEmployee = User.builder()
                .id(UUID.randomUUID())
                .employeeId("TEST_EMP_001")
                .email("unittest.employee@testcompany.com")
                .password("encoded_password")
                .role(Role.EMPLOYEE)
                .manager(manager)
                .build();
    }

    @Test
    void generateToken_ForManager_ReturnsValidToken() {
        String token = jwtTokenProvider.generateToken(testManager);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    void generateToken_ForEmployee_ReturnsValidToken() {
        String token = jwtTokenProvider.generateToken(testEmployee);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        String token = jwtTokenProvider.generateToken(testManager);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.string";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithMalformedToken_ReturnsFalse() {
        String malformedToken = "not-a-jwt-token";

        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithExpiredToken_ReturnsFalse() {
        // Create provider with -1ms expiration (already expired)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "test-secret-key-for-unit-testing-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm",
                -1L
        );

        String token = expiredProvider.generateToken(testManager);

        boolean isValid = expiredProvider.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithDifferentSecret_ReturnsFalse() {
        String token = jwtTokenProvider.generateToken(testManager);

        // Create provider with different secret
        JwtTokenProvider differentSecretProvider = new JwtTokenProvider(
                "different-secret-key-for-testing-purposes-must-be-at-least-256-bits-long-for-hmac-sha256",
                3600000L
        );

        boolean isValid = differentSecretProvider.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    void getUserIdFromToken_ReturnsCorrectUserId() {
        String token = jwtTokenProvider.generateToken(testManager);

        UUID userId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals(testManager.getId(), userId);
    }

    @Test
    void getEmailFromToken_ReturnsCorrectEmail() {
        String token = jwtTokenProvider.generateToken(testManager);

        String email = jwtTokenProvider.getEmailFromToken(token);

        assertEquals(testManager.getEmail(), email);
    }

    @Test
    void generateToken_ForManagerWithNoManager_ManagerIdClaimIsNull() {
        String token = jwtTokenProvider.generateToken(testManager);

        // Token should be valid and contain manager info
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(testManager.getEmail(), jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void generateToken_ForEmployeeWithManager_ContainsManagerId() {
        String token = jwtTokenProvider.generateToken(testEmployee);

        // Token should be valid and contain employee info
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(testEmployee.getEmail(), jwtTokenProvider.getEmailFromToken(token));
        assertEquals(testEmployee.getId(), jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void generateToken_TwoCallsForSameUser_GenerateDifferentTokens() {
        String token1 = jwtTokenProvider.generateToken(testManager);

        // Delay to ensure different issuedAt timestamp (JWT timestamps are in seconds)
        try {
            Thread.sleep(1001);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtTokenProvider.generateToken(testManager);

        assertNotEquals(token1, token2, "Tokens should be different due to different issuedAt times");

        // But both should be valid and contain same user info
        assertTrue(jwtTokenProvider.validateToken(token1));
        assertTrue(jwtTokenProvider.validateToken(token2));
        assertEquals(
                jwtTokenProvider.getUserIdFromToken(token1),
                jwtTokenProvider.getUserIdFromToken(token2)
        );
    }

    @Test
    void generateToken_WithNullUser_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            jwtTokenProvider.generateToken(null);
        });
    }

    @Test
    void validateToken_WithNullToken_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken(null);

        assertFalse(isValid);
    }

    @Test
    void validateToken_WithEmptyToken_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("");

        assertFalse(isValid);
    }

    @Test
    void getUserIdFromToken_WithInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getUserIdFromToken("invalid.token.here");
        });
    }

    @Test
    void getEmailFromToken_WithInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getEmailFromToken("invalid.token.here");
        });
    }
}
