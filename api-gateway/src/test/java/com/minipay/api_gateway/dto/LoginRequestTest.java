package com.minipay.api_gateway.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    private LoginRequest validRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret123");
        return request;
    }

    @Test
    void shouldHaveNoViolations_whenEmailAndPasswordAreValid() {
        LoginRequest request = validRequest();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailValidation_whenEmailIsBlank(String blankEmail) {
        LoginRequest request = validRequest();
        request.setEmail(blankEmail);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-at-sign.com", "user@", "@example.com"})
    void shouldFailValidation_whenEmailIsMalformed(String invalidEmail) {
        LoginRequest request = validRequest();
        request.setEmail(invalidEmail);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email must be valid");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailValidation_whenPasswordIsBlank(String blankPassword) {
        LoginRequest request = validRequest();
        request.setPassword(blankPassword);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Password is required");
    }

    @Test
    void shouldReportBothViolations_whenEmailAndPasswordAreBothBlank() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder("Email is required", "Password is required");
    }

    @Test
    void gettersAndSetters_shouldWorkAsExpected() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret123");

        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getPassword()).isEqualTo("secret123");
    }

    @Test
    void equalsAndHashCode_shouldBeConsistent_forIdenticalValues() {
        LoginRequest first = validRequest();
        LoginRequest second = validRequest();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}