package com.ccsanjuu.blog.modules.auth.model.dto;

import com.ccsanjuu.blog.modules.user.model.dto.ChangePasswordRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerPasswordShouldAllowHanLettersNumbersAndCommonSpecialChars() {
        assertPasswordValid("\u5bc6\u7801Aa1_!@#");

        String allowedSpecialChars = "!@#$%^&*()+=[]{}:;'\".,?/~`|\\<>-_";
        for (char ch : allowedSpecialChars.toCharArray()) {
            assertPasswordValid("Aa123" + ch);
        }
    }

    @Test
    void registerPasswordShouldRejectWhitespaceAndUnsupportedCharacters() {
        assertPasswordInvalid("Aa123 456");
        assertPasswordInvalid("Aa123\t456");
        assertPasswordInvalid("Aa123\uD83D\uDE00");
    }

    @Test
    void loginAndChangePasswordShouldUseSamePasswordPattern() {
        LoginRequestDTO loginRequest = new LoginRequestDTO("alice_dev", "Aa123 456");
        ChangePasswordRequestDTO changePasswordRequest = new ChangePasswordRequestDTO("Aa123!", "Aa123 456");

        assertFalse(VALIDATOR.validateProperty(loginRequest, "password").isEmpty());
        assertFalse(VALIDATOR.validateProperty(changePasswordRequest, "newPassword").isEmpty());
    }

    private static void assertPasswordValid(String password) {
        RegisterRequestDTO request = new RegisterRequestDTO("alice_dev", "alice@example.com", password);

        assertTrue(VALIDATOR.validateProperty(request, "password").isEmpty());
    }

    private static void assertPasswordInvalid(String password) {
        RegisterRequestDTO request = new RegisterRequestDTO("alice_dev", "alice@example.com", password);

        assertFalse(VALIDATOR.validateProperty(request, "password").isEmpty());
    }
}
