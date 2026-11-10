package com.ccsanjuu.blog.modules.user.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateProfileValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nicknameShouldAllowNullAndOneToTwentyCharacters() {
        assertNicknameValid(null);
        assertNicknameValid("A");
        assertNicknameValid("abcdefghijklmnopqrst");
    }

    @Test
    void nicknameShouldRejectEmptyOrTooLongValue() {
        assertNicknameInvalid("");
        assertNicknameInvalid("   ");
        assertNicknameInvalid("abcdefghijklmnopqrstu");
    }

    @Test
    void bioShouldAllowNullAndRejectTooLongValue() {
        UpdateProfileRequestDTO nullBioRequest = UpdateProfileRequestDTO.builder()
                .bio(null)
                .build();
        UpdateProfileRequestDTO maxLengthBioRequest = UpdateProfileRequestDTO.builder()
                .bio("a".repeat(100))
                .build();
        UpdateProfileRequestDTO tooLongBioRequest = UpdateProfileRequestDTO.builder()
                .bio("a".repeat(101))
                .build();

        assertTrue(VALIDATOR.validateProperty(nullBioRequest, "bio").isEmpty());
        assertTrue(VALIDATOR.validateProperty(maxLengthBioRequest, "bio").isEmpty());
        assertFalse(VALIDATOR.validateProperty(tooLongBioRequest, "bio").isEmpty());
    }

    private static void assertNicknameValid(String nickname) {
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .nickname(nickname)
                .build();

        assertTrue(VALIDATOR.validateProperty(request, "nickname").isEmpty());
    }

    private static void assertNicknameInvalid(String nickname) {
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .nickname(nickname)
                .build();

        assertFalse(VALIDATOR.validateProperty(request, "nickname").isEmpty());
    }
}
