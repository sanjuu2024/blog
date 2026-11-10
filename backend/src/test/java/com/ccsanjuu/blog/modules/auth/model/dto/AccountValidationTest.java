package com.ccsanjuu.blog.modules.auth.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void loginAccountShouldAllowUsernameOrEmail() {
        assertAccountValid("ab");
        assertAccountValid("_a");
        assertAccountValid("-x");
        assertAccountValid("alice_dev");
        assertAccountValid("a".repeat(20));
        assertAccountValid("alice.dev+blog@example.co");
        assertAccountValid("a@sub.example.com");
    }

    @Test
    void loginAccountShouldRejectInvalidUsernameAndEmail() {
        assertAccountInvalid("a");
        assertAccountInvalid("a".repeat(21));
        assertAccountInvalid("中文-user_01");
        assertAccountInvalid("alice user");
        assertAccountInvalid("alice.dev");
        assertAccountInvalid("alice@");
        assertAccountInvalid("alice@@example.com");
        assertAccountInvalid("alice@\u4f8b\u5b50.com");
        assertAccountInvalid(".alice@example.com");
    }

    private static void assertAccountValid(String account) {
        LoginRequestDTO request = new LoginRequestDTO(account, "Aa123!");

        assertTrue(VALIDATOR.validateProperty(request, "account").isEmpty());
    }

    private static void assertAccountInvalid(String account) {
        LoginRequestDTO request = new LoginRequestDTO(account, "Aa123!");

        assertFalse(VALIDATOR.validateProperty(request, "account").isEmpty());
    }
}
