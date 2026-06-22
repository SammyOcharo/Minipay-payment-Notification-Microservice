package com.minipay.api_gateway.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // Dummy target used only to obtain a real MethodParameter for WebExchangeBindException.
    private static class DummyTarget {
        @SuppressWarnings("unused")
        public void dummyMethod(String name) {
        }
    }

    private WebExchangeBindException buildBindException(String field, String message) throws NoSuchMethodException {
        DummyTarget target = new DummyTarget();
        BindingResult bindingResult = new BeanPropertyBindingResult(target, "dummyTarget");
        bindingResult.addError(new FieldError("dummyTarget", field, message));

        Method method = DummyTarget.class.getMethod("dummyMethod", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        return new WebExchangeBindException(parameter, bindingResult);
    }

    @Test
    void handleValidationErrors_shouldReturn400_withFieldErrorsInDetails() throws NoSuchMethodException {
        WebExchangeBindException ex = buildBindException("name", "Name is required");

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Validation failed");
        assertThat(response.getBody().get("timestamp")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertThat(details).containsEntry("name", "Name is required");
    }

    @Test
    void handleValidationErrors_shouldAggregateMultipleFieldErrors() throws NoSuchMethodException {
        DummyTarget target = new DummyTarget();
        BindingResult bindingResult = new BeanPropertyBindingResult(target, "dummyTarget");
        bindingResult.addError(new FieldError("dummyTarget", "name", "Name is required"));
        bindingResult.addError(new FieldError("dummyTarget", "email", "Email must be valid"));

        Method method = DummyTarget.class.getMethod("dummyMethod", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        WebExchangeBindException ex = new WebExchangeBindException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertThat(details)
                .containsEntry("name", "Name is required")
                .containsEntry("email", "Email must be valid");
    }

    @Test
    void handleUnauthorized_shouldReturn401_withMessageAndNoDetails() {
        UnauthorizedException ex = new UnauthorizedException("Invalid token");

        ResponseEntity<Map<String, Object>> response = handler.handleUnauthorized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid token");
        assertThat(response.getBody().get("timestamp")).isNotNull();
        assertThat(response.getBody()).doesNotContainKey("details");
    }

    @Test
    void handleIllegalArgument_shouldReturn400_withExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Bad input");
        assertThat(response.getBody()).doesNotContainKey("details");
    }

    @Test
    void handleGeneral_shouldReturn500_withGenericMessage_notLeakingExceptionDetails() {
        Exception ex = new RuntimeException("Some internal secret stack trace info");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody()).doesNotContainKey("details");
        // Important: the raw exception message should NOT be exposed to the client.
        assertThat(response.getBody().values()).doesNotContain("Some internal secret stack trace info");
    }
}