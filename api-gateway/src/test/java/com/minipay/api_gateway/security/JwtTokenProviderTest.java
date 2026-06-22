package com.minipay.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET =
            "minipay-super-secret-key-for-testing-purposes-min-256-bits";
    private static final long EXPIRATION = 86400000L;
    private static final long REFRESH_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(
                jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(
                jwtTokenProvider, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(
                jwtTokenProvider, "refreshExpiration", REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        String token = jwtTokenProvider
                .generateToken("test@minipay.com", "USER");

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        String token = jwtTokenProvider
                .generateToken("test@minipay.com", "USER");

        String email = jwtTokenProvider.extractEmail(token);

        assertThat(email).isEqualTo("test@minipay.com");
    }

    @Test
    @DisplayName("Should validate a valid token")
    void shouldValidateValidToken() {
        String token = jwtTokenProvider
                .generateToken("test@minipay.com", "USER");

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject an invalid token")
    void shouldRejectInvalidToken() {
        boolean isValid = jwtTokenProvider
                .validateToken("invalid.token.here");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(
                expiredProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(
                expiredProvider, "jwtExpiration", -1000L);
        ReflectionTestUtils.setField(
                expiredProvider, "refreshExpiration", -1000L);

        String expiredToken = expiredProvider
                .generateToken("test@minipay.com", "USER");

        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should generate different access and refresh tokens")
    void shouldGenerateDifferentAccessAndRefreshTokens() {
        String accessToken = jwtTokenProvider
                .generateToken("test@minipay.com", "USER");
        String refreshToken = jwtTokenProvider
                .generateRefreshToken("test@minipay.com");

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }
}