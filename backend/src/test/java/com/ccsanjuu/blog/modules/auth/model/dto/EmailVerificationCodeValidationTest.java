package com.ccsanjuu.blog.modules.auth.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationCodeValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void verificationCodeShouldRequireSixDigits() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "alice",
                "alice@example.com",
                "Aa123!"
        );
        request.setVerificationCode("123456");

        assertTrue(VALIDATOR.validateProperty(request, "verificationCode").isEmpty());

        request.setVerificationCode("12345a");
        assertFalse(VALIDATOR.validateProperty(request, "verificationCode").isEmpty());
    }
}
