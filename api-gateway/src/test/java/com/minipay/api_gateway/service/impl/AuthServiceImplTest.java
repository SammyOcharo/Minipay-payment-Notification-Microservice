package com.minipay.api_gateway.service.impl;

import com.minipay.api_gateway.dto.AuthResponse;
import com.minipay.api_gateway.dto.LoginRequest;
import com.minipay.api_gateway.dto.RegisterRequest;
import com.minipay.api_gateway.exception.UnauthorizedException;
import com.minipay.api_gateway.model.User;
import com.minipay.api_gateway.repository.UserRepository;
import com.minipay.api_gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@minipay.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhoneNumber("+254712345678");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@minipay.com");
        loginRequest.setPassword("password123");

        user = User.builder()
                .id("user-123")
                .fullName("John Doe")
                .email("john@minipay.com")
                .password("encoded-password")
                .phoneNumber("+254712345678")
                .role("USER")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldSuccessfullyRegisterNewUser() {
        // Arrange
        when(userRepository.existsByEmail("john@minipay.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateToken(anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString()))
                .thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@minipay.com");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getFullName()).isEqualTo("John Doe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate email")
    void shouldThrowExceptionForDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail("john@minipay.com"))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login with correct credentials")
    void shouldSuccessfullyLoginWithCorrectCredentials() {
        // Arrange
        when(userRepository.findByEmail("john@minipay.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password"))
                .thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString()))
                .thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("john@minipay.com");
    }

    @Test
    @DisplayName("Should throw exception for wrong password")
    void shouldThrowExceptionForWrongPassword() {
        // Arrange
        when(userRepository.findByEmail("john@minipay.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void shouldThrowExceptionForNonExistentUser() {
        // Arrange
        when(userRepository.findByEmail("john@minipay.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw exception for disabled account")
    void shouldThrowExceptionForDisabledAccount() {
        // Arrange
        user.setEnabled(false);
        when(userRepository.findByEmail("john@minipay.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Account is disabled");
    }
}