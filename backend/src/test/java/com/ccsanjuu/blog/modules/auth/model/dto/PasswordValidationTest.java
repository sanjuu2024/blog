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
    void registerPasswordShouldAllowLengthBoundariesAndCommonAsciiSpecialChars() {
        assertPasswordValid("Aa123!");
        assertPasswordValid("A".repeat(32));

        String allowedSpecialChars = "!@#$%^&*()+=[]{}:;'\".,?/~`|\\<>-_";
        for (char ch : allowedSpecialChars.toCharArray()) {
            assertPasswordValid("Aa123" + ch);
        }
    }

    @Test
    void registerPasswordShouldRejectInvalidLengthWhitespaceAndUnsupportedCharacters() {
        assertPasswordInvalid("Aa12!");
        assertPasswordInvalid("A".repeat(33));
        assertPasswordInvalid("密码Aa1!@");
        assertPasswordInvalid("Aa123 456");
        assertPasswordInvalid("Aa123\t456");
        assertPasswordInvalid("Aa123\uD83D\uDE00");
    }

    @Test
    void loginAndChangePasswordShouldUseSamePasswordPattern() {
        LoginRequestDTO validLoginRequest = new LoginRequestDTO("alice_dev", "A".repeat(32));
        LoginRequestDTO invalidLoginRequest = new LoginRequestDTO("alice_dev", "密码Aa1!@");
        ChangePasswordRequestDTO validChangeRequest =
                new ChangePasswordRequestDTO("A".repeat(32), "A".repeat(32));
        ChangePasswordRequestDTO invalidChangeRequest =
                new ChangePasswordRequestDTO("密码Aa1!@", "Aa123 456");

        assertTrue(VALIDATOR.validateProperty(validLoginRequest, "password").isEmpty());
        assertFalse(VALIDATOR.validateProperty(invalidLoginRequest, "password").isEmpty());
        assertTrue(VALIDATOR.validateProperty(validChangeRequest, "oldPassword").isEmpty());
        assertTrue(VALIDATOR.validateProperty(validChangeRequest, "newPassword").isEmpty());
        assertFalse(VALIDATOR.validateProperty(invalidChangeRequest, "oldPassword").isEmpty());
        assertFalse(VALIDATOR.validateProperty(invalidChangeRequest, "newPassword").isEmpty());
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
