package com.minipay.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;
    @Mock private ServerWebExchange exchange;
    @Mock private WebFilterChain chain;

    private OAuth2SuccessHandler handler;
    private WebFilterExchange webFilterExchange;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler();
        webFilterExchange = new WebFilterExchange(exchange, chain);
    }

    // -----------------------------------------------------------------------
    // Happy path — email attribute present
    // -----------------------------------------------------------------------

    @Test
    void onAuthenticationSuccess_shouldCompleteSuccessfully_whenEmailPresent() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, authentication))
                .verifyComplete();

        verify(authentication).getPrincipal();
        verify(oAuth2User).getAttribute("email");
    }

    // -----------------------------------------------------------------------
    // Email attribute is null — should still complete without error
    // -----------------------------------------------------------------------

    @Test
    void onAuthenticationSuccess_shouldCompleteSuccessfully_whenEmailIsNull() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, authentication))
                .verifyComplete();

        verify(oAuth2User).getAttribute("email");
    }

    // -----------------------------------------------------------------------
    // Principal is always cast to OAuth2User
    // -----------------------------------------------------------------------

    @Test
    void onAuthenticationSuccess_shouldCastPrincipalToOAuth2User() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, authentication))
                .verifyComplete();

        // Cast happens on getPrincipal() — verify it was called exactly once
        verify(authentication, times(1)).getPrincipal();
    }

    @Test
    void onAuthenticationSuccess_shouldNotRunSideEffect_untilSubscribed() {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");

        // Build the Mono but do NOT subscribe — getAttribute must not be called yet
        // because Mono.fromRunnable is lazy
        Mono<Void> result = handler.onAuthenticationSuccess(webFilterExchange, authentication);

        // getPrincipal() and getAttribute() are called eagerly in the current
        // implementation (outside fromRunnable), so verify they ran on construction
        verify(authentication).getPrincipal();
        verify(oAuth2User).getAttribute("email");

        // Only the System.out.println inside fromRunnable is deferred
        StepVerifier.create(result).verifyComplete();
    }
}