package com.ccsanjuu.blog.modules.auth.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerEmailShouldAllowCommonEmailFormats() {
        assertEmailValid("alice@example.com");
        assertEmailValid("alice.dev+blog@example.co");
        assertEmailValid("a@sub.example.com");
    }

    @Test
    void registerEmailShouldRejectWhitespaceHanAndIncompleteDomains() {
        assertEmailInvalid("alice example@example.com");
        assertEmailInvalid("alice@\u4f8b\u5b50.com");
        assertEmailInvalid("alice@localhost");
        assertEmailInvalid("alice@example");
    }

    @Test
    void registerEmailShouldRejectInvalidLocalOrDomainLabels() {
        assertEmailInvalid(".alice@example.com");
        assertEmailInvalid("alice.@example.com");
        assertEmailInvalid("alice@-example.com");
        assertEmailInvalid("alice@example-.com");
    }

    private static void assertEmailValid(String email) {
        RegisterRequestDTO request = new RegisterRequestDTO("alice_dev", email, "Aa123!");

        assertTrue(VALIDATOR.validateProperty(request, "email").isEmpty());
    }

    private static void assertEmailInvalid(String email) {
        RegisterRequestDTO request = new RegisterRequestDTO("alice_dev", email, "Aa123!");

        assertFalse(VALIDATOR.validateProperty(request, "email").isEmpty());
    }
}
