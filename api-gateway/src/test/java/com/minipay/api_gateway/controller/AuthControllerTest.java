package com.minipay.api_gateway.controller;

import com.minipay.api_gateway.dto.AuthResponse;
import com.minipay.api_gateway.dto.LoginRequest;
import com.minipay.api_gateway.dto.RegisterRequest;
import com.minipay.api_gateway.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;

    private static final AuthResponse AUTH_RESPONSE = AuthResponse.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .email("user@example.com")
            .fullName("Test User")
            .role("USER")
            .expiresIn(86400000L)
            .build();


    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    // -----------------------------------------------------------------------
    // POST /register — happy path
    // -----------------------------------------------------------------------

    @Test
    void register_shouldReturn201WithBody_whenRegistrationSucceeds() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("+254712345678");
        when(authService.register(request)).thenReturn(AUTH_RESPONSE);

        StepVerifier.create(controller.register(request))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(response.getBody()).isEqualTo(AUTH_RESPONSE);
                })
                .verifyComplete();

        verify(authService).register(request);
    }

    @Test
    void register_shouldReturn201_notOk200() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("+254712345678");
        when(authService.register(request)).thenReturn(AUTH_RESPONSE);

        StepVerifier.create(controller.register(request))
                .assertNext(response ->
                        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK)
                )
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // POST /register — service throws
    // -----------------------------------------------------------------------

    @Test
    void register_shouldPropagateException_whenServiceThrows() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("+254712345678");
        when(authService.register(request))
                .thenThrow(new RuntimeException("Email already registered"));

        StepVerifier.create(controller.register(request))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(RuntimeException.class);
                    assertThat(ex.getMessage()).isEqualTo("Email already registered");
                })
                .verify();

        verify(authService).register(request);
    }

    @Test
    void login_shouldReturn200WithBody_whenLoginSucceeds() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        when(authService.login(request)).thenReturn(AUTH_RESPONSE);

        StepVerifier.create(controller.login(request))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(AUTH_RESPONSE);
                })
                .verifyComplete();

        verify(authService).login(request);
    }

    @Test
    void login_shouldReturnOk200_notCreated201() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        when(authService.login(request)).thenReturn(AUTH_RESPONSE);

        StepVerifier.create(controller.login(request))
                .assertNext(response ->
                        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.CREATED)
                )
                .verifyComplete();
    }

    // -----------------------------------------------------------------------
    // POST /login — service throws
    // -----------------------------------------------------------------------

    @Test
    void login_shouldPropagateException_whenServiceThrows() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        when(authService.login(request))
                .thenThrow(new RuntimeException("Invalid credentials"));

        StepVerifier.create(controller.login(request))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(RuntimeException.class);
                    assertThat(ex.getMessage()).isEqualTo("Invalid credentials");
                })
                .verify();

        verify(authService).login(request);
    }

    @Test
    void register_shouldReturnExactServiceResponse_withoutTransformation() {
        AuthResponse specificResponse = AuthResponse.builder()
                .accessToken("specific-access")
                .refreshToken("specific-refresh")
                .build();
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("+254712345678");
        when(authService.register(request)).thenReturn(specificResponse);

        StepVerifier.create(controller.register(request))
                .assertNext(response ->
                        assertThat(response.getBody()).isSameAs(specificResponse)
                )
                .verifyComplete();
    }

    @Test
    void login_shouldReturnExactServiceResponse_withoutTransformation() {
        AuthResponse specificResponse = AuthResponse.builder()
                .accessToken("specific-access")
                .refreshToken("specific-refresh")
                .build();
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        when(authService.login(request)).thenReturn(specificResponse);

        StepVerifier.create(controller.login(request))
                .assertNext(response ->
                        assertThat(response.getBody()).isSameAs(specificResponse)
                )
                .verifyComplete();
    }
}