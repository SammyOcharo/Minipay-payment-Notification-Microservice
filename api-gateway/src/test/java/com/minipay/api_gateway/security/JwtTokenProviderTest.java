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

    @Test
    @DisplayName("Should reject a malformed token")
    void shouldRejectMalformedToken() {
        // A string that looks like a JWT structure but has corrupted segments
        boolean isValid = jwtTokenProvider.validateToken("not.a.validjwt");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject a token signed with a different key")
    void shouldRejectTokenSignedWithDifferentKey() {
        // Generates a token with a completely different secret —
        // parseClaims() will throw JwtException (signature mismatch)
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(
                otherProvider, "jwtSecret",
                "completely-different-secret-key-that-is-at-least-256-bits-long!!");
        ReflectionTestUtils.setField(otherProvider, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(otherProvider, "refreshExpiration", REFRESH_EXPIRATION);

        String foreignToken = otherProvider.generateToken("test@minipay.com", "USER");

        boolean isValid = jwtTokenProvider.validateToken(foreignToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject an unsigned (unsecured) token")
    void shouldRejectUnsignedToken() {
        // A well-formed JWT with header.payload but no signature — triggers
        // UnsupportedJwtException because the parser requires a signed token
        String unsignedToken = "eyJhbGciOiJub25lIn0"   // {"alg":"none"}
                + ".eyJzdWIiOiJ0ZXN0QG1pbmlwYXkuY29tIn0" // {"sub":"test@minipay.com"}
                + ".";

        boolean isValid = jwtTokenProvider.validateToken(unsignedToken);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject a token with a tampered payload")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken("test@minipay.com", "USER");

        // Split, replace the payload segment with a different base64 block,
        // then reassemble — signature no longer matches the payload
        String[] parts = token.split("\\.");
        String tamperedPayload = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"sub\":\"hacker@evil.com\"}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);
        assertThat(isValid).isFalse();
    }
}