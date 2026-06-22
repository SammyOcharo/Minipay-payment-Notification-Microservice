package com.minipay.api_gateway.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void builder_shouldDefaultTokenTypeToBearer_whenNotExplicitlySet() {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-123")
                .refreshToken("refresh-456")
                .expiresIn(3600L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .role("USER")
                .build();

        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void builder_shouldAllowOverridingDefaultTokenType() {
        AuthResponse response = AuthResponse.builder()
                .tokenType("Basic")
                .build();

        assertThat(response.getTokenType()).isEqualTo("Basic");
    }

    @Test
    void builder_shouldSetAllFieldsCorrectly() {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-123")
                .refreshToken("refresh-456")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .role("ADMIN")
                .build();

        assertThat(response.getAccessToken()).isEqualTo("access-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-456");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void noArgsConstructor_withSetters_shouldUpdateFields() {
        AuthResponse response = new AuthResponse();
        response.setAccessToken("access-789");
        response.setTokenType("Bearer");
        response.setExpiresIn(1800L);

        assertThat(response.getAccessToken()).isEqualTo("access-789");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(1800L);
    }

    @Test
    void allArgsConstructor_shouldSetAllFieldsInOrder() {
        AuthResponse response = new AuthResponse(
                "access-123",
                "refresh-456",
                "Bearer",
                3600L,
                "user@example.com",
                "Jane Doe",
                "USER"
        );

        assertThat(response.getAccessToken()).isEqualTo("access-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-456");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void equalsAndHashCode_shouldBeConsistent_forIdenticalValues() {
        AuthResponse first = AuthResponse.builder()
                .accessToken("access-123")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        AuthResponse second = AuthResponse.builder()
                .accessToken("access-123")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_shouldReturnFalse_forDifferentValues() {
        AuthResponse first = AuthResponse.builder().accessToken("access-123").build();
        AuthResponse second = AuthResponse.builder().accessToken("access-999").build();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void toString_shouldContainKeyFieldValues() {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-123")
                .email("user@example.com")
                .build();

        String result = response.toString();

        assertThat(result).contains("access-123");
        assertThat(result).contains("user@example.com");
    }
}