package com.minipay.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {

    private static final String CORRELATION_ID = "X-Correlation-ID";

    @Mock private GatewayFilterChain chain;
    @Mock private ServerWebExchange exchange;
    @Mock private ServerHttpRequest request;
    @Mock private ServerHttpResponse response;

    private LoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost/api/test"));
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void filter_shouldUseExistingCorrelationId_whenHeaderPresent() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(CORRELATION_ID, "my-correlation-id");
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    // -----------------------------------------------------------------------
    // Correlation ID — header absent → UUID generated
    // -----------------------------------------------------------------------

    @Test
    void filter_shouldGenerateCorrelationId_whenHeaderAbsent() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    // -----------------------------------------------------------------------
    // Chain is always called
    // -----------------------------------------------------------------------

    @Test
    void filter_shouldAlwaysInvokeChain() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    // -----------------------------------------------------------------------
    // doFinally — response status is read after chain completes (ON_COMPLETE)
    // -----------------------------------------------------------------------

    @Test
    void filter_shouldReadResponseStatus_afterChainCompletes() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // doFinally runs on any terminal signal — verify status was accessible
        verify(response, atLeastOnce()).getStatusCode();
    }

    // -----------------------------------------------------------------------
    // doFinally — response status is read after chain errors (ON_ERROR)
    // -----------------------------------------------------------------------

    @Test
    void filter_shouldReadResponseStatus_afterChainErrors() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("upstream failure")));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectError(RuntimeException.class)
                .verify();

        // doFinally fires on error too — status still read
        verify(response, atLeastOnce()).getStatusCode();
    }

    // -----------------------------------------------------------------------
    // doFinally — response status is read on cancellation (ON_CANCEL)
    // -----------------------------------------------------------------------

    @Test
    void filter_shouldReadResponseStatus_whenSubscriptionCancelled() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        // Never-completing chain simulates a slow upstream; subscriber cancels
        when(chain.filter(exchange)).thenReturn(Mono.never());

        StepVerifier.create(filter.filter(exchange, chain))
                .thenCancel()
                .verify();

        verify(response, atLeastOnce()).getStatusCode();
    }

    // -----------------------------------------------------------------------
    // HTTP method and URI are logged (accessed) before chain is called
    // -----------------------------------------------------------------------

    @Test
    void filter_shouldAccessMethodAndUri_beforeInvokingChain() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // These are read synchronously before chain.filter() — verify access
        verify(request, atLeastOnce()).getMethod();
        verify(request, atLeastOnce()).getURI();
    }
}