package com.minipay.notification_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // -----------------------------------------------------------------------
    // ResponseStatusException — status code forwarded
    // -----------------------------------------------------------------------

    @Test
    void handleResponseStatus_shouldReturnStatusFromException() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Resource not found");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleResponseStatus_shouldReturnReasonInBody() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Resource not found");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getBody()).containsEntry("message", "Resource not found");
    }

    @Test
    void handleResponseStatus_shouldIncludeTimestampInBody() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid input");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void handleResponseStatus_shouldReturn400_whenBadRequestThrown() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Bad request");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleResponseStatus_shouldReturn403_whenForbiddenThrown() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Access denied");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "Access denied");
    }

    @Test
    void handleResponseStatus_shouldReturn401_whenUnauthorizedThrown() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Unauthorized");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("message", "Unauthorized");
    }

    // -----------------------------------------------------------------------
    // ResponseStatusException — null reason
    // -----------------------------------------------------------------------

    @Test
    void handleResponseStatus_shouldHandleNullReason_withoutThrowing() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }

    // -----------------------------------------------------------------------
    // General Exception — always 500
    // -----------------------------------------------------------------------

    @Test
    void handleGeneral_shouldReturn500_forAnyException() {
        Exception ex = new Exception("Something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleGeneral_shouldReturnGenericMessageInBody() {
        Exception ex = new Exception("Something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getBody())
                .containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void handleGeneral_shouldIncludeTimestampInBody() {
        Exception ex = new Exception("Something went wrong");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void handleGeneral_shouldReturn500_forRuntimeException() {
        RuntimeException ex = new RuntimeException("Null pointer");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void handleGeneral_shouldNotExposeInternalExceptionMessage() {
        Exception ex = new Exception("Sensitive DB error details");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        // Internal exception message must never leak to the client
        assertThat(response.getBody().get("message"))
                .isNotEqualTo("Sensitive DB error details");
        assertThat(response.getBody())
                .containsEntry("message", "An unexpected error occurred");
    }

    // -----------------------------------------------------------------------
    // buildError — body structure always has exactly two keys
    // -----------------------------------------------------------------------

    @Test
    void handleGeneral_shouldReturnBodyWithExactlyTwoKeys() {
        Exception ex = new Exception("error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsKeys("timestamp", "message");
    }

    @Test
    void handleResponseStatus_shouldReturnBodyWithExactlyTwoKeys() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Not found");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsKeys("timestamp", "message");
    }

    // -----------------------------------------------------------------------
    // timestamp — is a valid LocalDateTime string
    // -----------------------------------------------------------------------

    @Test
    void handleGeneral_shouldReturnValidTimestampFormat() {
        Exception ex = new Exception("error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        String timestamp = (String) response.getBody().get("timestamp");
        // Should parse without throwing — format is LocalDateTime.now().toString()
        assertThat(timestamp).isNotBlank();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                java.time.LocalDateTime.parse(timestamp)
        ).doesNotThrowAnyException();
    }

    @Test
    void handleResponseStatus_shouldReturnValidTimestampFormat() {
        ResponseStatusException ex = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Bad input");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        String timestamp = (String) response.getBody().get("timestamp");
        assertThat(timestamp).isNotBlank();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                java.time.LocalDateTime.parse(timestamp)
        ).doesNotThrowAnyException();
    }
}