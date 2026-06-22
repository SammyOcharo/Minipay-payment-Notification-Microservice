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

class RegisterRequestTest {

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

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("user@example.com");
        request.setPassword("secret123");
        request.setPhoneNumber("+254712345678");
        return request;
    }

    @Test
    void shouldHaveNoViolations_whenAllFieldsAreValid() {
        RegisterRequest request = validRequest();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ---- fullName ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailValidation_whenFullNameIsBlank(String blankName) {
        RegisterRequest request = validRequest();
        request.setFullName(blankName);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Full name is required");
    }

    @Test
    void shouldFailValidation_whenFullNameIsBelowMinSize() {
        RegisterRequest request = validRequest();
        request.setFullName("A"); // length 1, min is 2

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Full name must be between 2 and 100 characters");
    }

    @Test
    void shouldFailValidation_whenFullNameExceedsMaxSize() {
        RegisterRequest request = validRequest();
        request.setFullName("A".repeat(101)); // max is 100

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Full name must be between 2 and 100 characters");
    }

    @Test
    void shouldPassValidation_whenFullNameIsAtMinAndMaxBoundaries() {
        RegisterRequest atMin = validRequest();
        atMin.setFullName("Jo"); // length 2

        RegisterRequest atMax = validRequest();
        atMax.setFullName("A".repeat(100)); // length 100

        assertThat(validator.validate(atMin)).isEmpty();
        assertThat(validator.validate(atMax)).isEmpty();
    }

    // ---- email ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailValidation_whenEmailIsBlank(String blankEmail) {
        RegisterRequest request = validRequest();
        request.setEmail(blankEmail);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing-at-sign.com", "user@", "@example.com"})
    void shouldFailValidation_whenEmailIsMalformed(String invalidEmail) {
        RegisterRequest request = validRequest();
        request.setEmail(invalidEmail);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email must be valid");
    }

    // ---- password ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailValidation_whenPasswordIsBlank(String blankPassword) {
        RegisterRequest request = validRequest();
        request.setPassword(blankPassword);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Password is required");
    }

    @Test
    void shouldFailValidation_whenPasswordIsBelowMinLength() {
        RegisterRequest request = validRequest();
        request.setPassword("short1"); // length 6, min is 8

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Password must be at least 8 characters");
    }

    @Test
    void shouldPassValidation_whenPasswordIsExactlyMinLength() {
        RegisterRequest request = validRequest();
        request.setPassword("12345678"); // exactly 8

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ---- phoneNumber ----

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldFailValidation_whenPhoneNumberIsBlank(String blankPhone) {
        RegisterRequest request = validRequest();
        request.setPhoneNumber(blankPhone);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Phone number is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0712345678",      // missing country code / leading zero not allowed
            "+0712345678",     // cannot start with 0 after +
            "phone123",        // not numeric
            "+254-712-345678", // contains hyphens
            "12345678901234567" // too long (> 15 digits)
    })
    void shouldFailValidation_whenPhoneNumberIsMalformed(String invalidPhone) {
        RegisterRequest request = validRequest();
        request.setPhoneNumber(invalidPhone);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Phone number must be valid e.g +254712345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+254712345678", "254712345678", "+12025550123"})
    void shouldPassValidation_whenPhoneNumberIsValid(String validPhone) {
        RegisterRequest request = validRequest();
        request.setPhoneNumber(validPhone);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void gettersAndSetters_shouldWorkAsExpected() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("user@example.com");
        request.setPassword("secret123");
        request.setPhoneNumber("+254712345678");

        assertThat(request.getFullName()).isEqualTo("Jane Doe");
        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getPassword()).isEqualTo("secret123");
        assertThat(request.getPhoneNumber()).isEqualTo("+254712345678");
    }

    @Test
    void equalsAndHashCode_shouldBeConsistent_forIdenticalValues() {
        RegisterRequest first = validRequest();
        RegisterRequest second = validRequest();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}