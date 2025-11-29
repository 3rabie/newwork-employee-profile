package com.newwork.employee.service;

import com.newwork.employee.dto.request.LoginRequest;
import com.newwork.employee.dto.response.AuthResponse;
import com.newwork.employee.entity.User;
import com.newwork.employee.entity.enums.Role;
import com.newwork.employee.exception.InvalidCredentialsException;
import com.newwork.employee.mapper.AuthMapper;
import com.newwork.employee.repository.UserRepository;
import com.newwork.employee.security.JwtTokenProvider;
import com.newwork.employee.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Uses mocks to test business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest loginRequest;
    private AuthResponse expectedResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .employeeId("TEST_001")
                .email("test.user@testcompany.com")
                .password("encoded_password")
                .role(Role.EMPLOYEE)
                .build();

        loginRequest = new LoginRequest("test.user@testcompany.com", "TestPassword123!");

        expectedResponse = AuthResponse.builder()
                .token("mock.jwt.token")
                .userId(testUser.getId())
                .email(testUser.getEmail())
                .employeeId(testUser.getEmployeeId())
                .role(testUser.getRole().name())
                .managerId(null)
                .build();
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        void withValidCredentials_ReturnsAuthResponse() {
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(testUser))
                    .thenReturn("mock.jwt.token");
            when(authMapper.toAuthResponse(testUser, "mock.jwt.token"))
                    .thenReturn(expectedResponse);

            AuthResponse result = authService.login(loginRequest);

            assertNotNull(result);
            assertEquals(expectedResponse.getToken(), result.getToken());
            assertEquals(expectedResponse.getEmail(), result.getEmail());
            assertEquals(expectedResponse.getUserId(), result.getUserId());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository).findByEmail(loginRequest.getEmail());
            verify(jwtTokenProvider).generateToken(testUser);
            verify(authMapper).toAuthResponse(testUser, "mock.jwt.token");
        }

        @Test
        void withInvalidCredentials_ThrowsException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository, never()).findByEmail(any());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        void whenUserNotFoundAfterAuth_ThrowsException() {
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));

            assertTrue(exception.getMessage().contains("User not found"));
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository).findByEmail(loginRequest.getEmail());
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        void callsAuthMapperWithCorrectParameters() {
            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(testUser))
                    .thenReturn("generated.token.value");
            when(authMapper.toAuthResponse(testUser, "generated.token.value"))
                    .thenReturn(expectedResponse);

            authService.login(loginRequest);

            verify(authMapper).toAuthResponse(eq(testUser), eq("generated.token.value"));
        }
    }

    @Nested
    @DisplayName("Switch User")
    class SwitchUserTests {

        @Test
        void withValidEmail_ReturnsAuthResponse() {
            String email = "test.user@testcompany.com";
            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(testUser))
                    .thenReturn("mock.jwt.token");
            when(authMapper.toAuthResponse(testUser, "mock.jwt.token"))
                    .thenReturn(expectedResponse);

            AuthResponse result = authService.switchUser(email);

            assertNotNull(result);
            assertEquals(expectedResponse.getToken(), result.getToken());
            assertEquals(expectedResponse.getEmail(), result.getEmail());

            verify(userRepository).findByEmail(email);
            verify(jwtTokenProvider).generateToken(testUser);
            verify(authMapper).toAuthResponse(testUser, "mock.jwt.token");
            verifyNoInteractions(authenticationManager);
        }

        @Test
        void withNonExistentEmail_ThrowsException() {
            String email = "nonexistent@testcompany.com";
            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.switchUser(email));

            assertTrue(exception.getMessage().contains("User not found"));
            verify(userRepository).findByEmail(email);
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        void doesNotRequirePassword() {
            String email = "test.user@testcompany.com";
            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(testUser))
                    .thenReturn("mock.jwt.token");
            when(authMapper.toAuthResponse(testUser, "mock.jwt.token"))
                    .thenReturn(expectedResponse);

            authService.switchUser(email);

            verifyNoInteractions(authenticationManager);
        }

        @Test
        void callsAuthMapperWithCorrectParameters() {
            String email = "test.user@testcompany.com";
            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(testUser))
                    .thenReturn("switched.token.value");
            when(authMapper.toAuthResponse(testUser, "switched.token.value"))
                    .thenReturn(expectedResponse);

            authService.switchUser(email);

            verify(authMapper).toAuthResponse(eq(testUser), eq("switched.token.value"));
        }
    }
}
