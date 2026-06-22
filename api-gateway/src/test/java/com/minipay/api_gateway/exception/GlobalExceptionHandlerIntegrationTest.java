package com.minipay.api_gateway.exception;


import com.minipay.api_gateway.security.CustomUserDetailsService;
import com.minipay.api_gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;


/**
 * Integration test: boots a minimal WebFlux slice with a throwaway controller
 * (ExceptionTestController) so each exception path is exercised through a
 * real HTTP request/response, with GlobalExceptionHandler wired in exactly
 * as it would be in production.
 */
@WebFluxTest(
        controllers = ExceptionTestController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class,
                ReactiveOAuth2ClientAutoConfiguration.class,
                ReactiveOAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerIntegrationTest {

    // JwtAuthenticationFilter is a WebFilter, so @WebFluxTest still pulls it into
    // the slice context even with security autoconfiguration excluded. It needs
    // these collaborators satisfied or the context fails to start.
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn401_whenUnauthorizedExceptionIsThrown() {
        webTestClient.get().uri("/test/unauthorized")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid token")
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.details").doesNotExist();
    }

    @Test
    void shouldReturn400_whenIllegalArgumentExceptionIsThrown() {
        webTestClient.get().uri("/test/illegal-argument")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Bad input")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void shouldReturn500_withGenericMessage_whenUnexpectedExceptionIsThrown() {
        webTestClient.get().uri("/test/general-error")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message").isEqualTo("An unexpected error occurred")
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.details").doesNotExist();
    }

    @Test
    void shouldReturn400_withFieldErrors_whenRequestBodyFailsValidation() {
        ExceptionTestRequest invalidRequest = new ExceptionTestRequest();
        invalidRequest.setName(""); // blank -> violates @NotBlank

        webTestClient.post().uri("/test/validate")
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Validation failed")
                .jsonPath("$.details.name").isEqualTo("Name is required");
    }

    @Test
    void shouldReturn200_whenRequestBodyIsValid() {
        ExceptionTestRequest validRequest = new ExceptionTestRequest();
        validRequest.setName("Jane Doe");

        webTestClient.post().uri("/test/validate")
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isOk();
    }
}
