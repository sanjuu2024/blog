package com.ccsanjuu.blog.modules.auth.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernameValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerUsernameShouldAllowLengthBoundariesAndSupportedCharacters() {
        assertUsernameValid("ab");
        assertUsernameValid("_a");
        assertUsernameValid("-x");
        assertUsernameValid("a".repeat(20));
        assertUsernameValid("Alice_01-test");
    }

    @Test
    void registerUsernameShouldRejectInvalidLengthAndUnsupportedCharacters() {
        assertUsernameInvalid("a");
        assertUsernameInvalid("a".repeat(21));
        assertUsernameInvalid("中文用户名");
        assertUsernameInvalid("alice user");
        assertUsernameInvalid("alice.dev");
    }

    private static void assertUsernameValid(String username) {
        RegisterRequestDTO request = new RegisterRequestDTO(username, "alice@example.com", "Aa123!");

        assertTrue(VALIDATOR.validateProperty(request, "username").isEmpty());
    }

    private static void assertUsernameInvalid(String username) {
        RegisterRequestDTO request = new RegisterRequestDTO(username, "alice@example.com", "Aa123!");

        assertFalse(VALIDATOR.validateProperty(request, "username").isEmpty());
    }
}
