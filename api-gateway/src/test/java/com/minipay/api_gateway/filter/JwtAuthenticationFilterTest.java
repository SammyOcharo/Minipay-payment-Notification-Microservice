package com.minipay.api_gateway.filter;


import com.minipay.api_gateway.security.CustomUserDetailsService;
import com.minipay.api_gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private WebFilterChain chain;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
        when(exchange.getRequest()).thenReturn(request);
    }

    private void givenAuthorizationHeader(String value) {
        HttpHeaders headers = new HttpHeaders();
        if (value != null) {
            headers.set(HttpHeaders.AUTHORIZATION, value);
        }
        when(request.getHeaders()).thenReturn(headers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "Basic dXNlcjpwYXNz", // wrong scheme entirely
            "Bearer"              // missing the required trailing space/token
    })
    void filter_shouldDelegateDirectlyToChain_whenNoValidBearerTokenPresent(String headerValue) {
        givenAuthorizationHeader(headerValue);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtTokenProvider);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void filter_shouldDelegateToChain_whenTokenFailsValidation() {
        givenAuthorizationHeader("Bearer bad-token");
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtTokenProvider).validateToken("bad-token");
        verify(jwtTokenProvider, never()).extractEmail(any());
        verify(chain).filter(exchange);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void filter_shouldCompleteWithoutInvokingChain_whenTokenValidButUserNotFound() {
        // Documents current behavior: if findByUsername() returns empty,
        // flatMap's mapper never runs, so chain.filter(exchange) is never
        // called and the request silently completes with no response written.
        // This may be an unintended bug worth revisiting in the production code.
        givenAuthorizationHeader("Bearer good-token");
        when(jwtTokenProvider.validateToken("good-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("good-token")).thenReturn("ghost@example.com");
        when(userDetailsService.findByUsername("ghost@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(userDetailsService).findByUsername("ghost@example.com");
        verifyNoInteractions(chain);
    }

    @Test
    void filter_shouldSetAuthenticationInContext_whenTokenValidAndUserFound() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities())
                .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_USER")));

        givenAuthorizationHeader("Bearer good-token");
        when(jwtTokenProvider.validateToken("good-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("good-token")).thenReturn("user@example.com");
        when(userDetailsService.findByUsername("user@example.com")).thenReturn(Mono.just(userDetails));

        AtomicReference<Authentication> capturedAuth = new AtomicReference<>();
        when(chain.filter(exchange)).thenReturn(
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(ctx -> capturedAuth.set(ctx.getAuthentication()))
                        .then()
        );

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(jwtTokenProvider).validateToken("good-token");
        verify(jwtTokenProvider).extractEmail("good-token");
        verify(userDetailsService).findByUsername("user@example.com");
        verify(chain).filter(exchange);

        Authentication auth = capturedAuth.get();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isSameAs(userDetails);
        assertThat(auth.getCredentials()).isNull();
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void filter_shouldPropagateAllAuthorities_whenUserHasMultipleRoles() {
        UserDetails adminUser = mock(UserDetails.class);
        when(adminUser.getAuthorities()).thenAnswer(inv -> List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));

        givenAuthorizationHeader("Bearer admin-token");
        when(jwtTokenProvider.validateToken("admin-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("admin-token")).thenReturn("admin@example.com");
        when(userDetailsService.findByUsername("admin@example.com")).thenReturn(Mono.just(adminUser));

        AtomicReference<Authentication> capturedAuth = new AtomicReference<>();
        when(chain.filter(exchange)).thenReturn(
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(ctx -> capturedAuth.set(ctx.getAuthentication()))
                        .then()
        );

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(capturedAuth.get().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void filter_shouldNotLeakAuthenticationToOuterContext_whenChainCompletes() {
        UserDetails userDetails = mock(UserDetails.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

        givenAuthorizationHeader("Bearer scoped-token");
        when(jwtTokenProvider.validateToken("scoped-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("scoped-token")).thenReturn("user@example.com");
        when(userDetailsService.findByUsername("user@example.com")).thenReturn(Mono.just(userDetails));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        AtomicReference<Authentication> outsideAuth = new AtomicReference<>();

        Mono<Void> pipeline = filter.filter(exchange, chain)
                .then(ReactiveSecurityContextHolder.getContext()
                        .doOnNext(ctx -> outsideAuth.set(ctx.getAuthentication()))
                        .then()
                )
                .onErrorResume(e -> Mono.empty());

        StepVerifier.create(pipeline).verifyComplete();

        assertThat(outsideAuth.get()).isNull();
    }
}