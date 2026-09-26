package com.ccsanjuu.blog.modules.auth.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacyPolicyVersionValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerPrivacyPolicyVersionShouldRequireSha256Format() {
        RegisterRequestDTO validRequest = new RegisterRequestDTO(
                "alice",
                "alice@example.com",
                "Aa123!"
        );
        validRequest.setPrivacyPolicyVersion("sha256:" + "a".repeat(64));
        assertTrue(VALIDATOR.validateProperty(validRequest, "privacyPolicyVersion").isEmpty());

        validRequest.setPrivacyPolicyVersion("sha256:ABC" + "a".repeat(61));
        assertFalse(VALIDATOR.validateProperty(validRequest, "privacyPolicyVersion").isEmpty());
    }

    @Test
    void registerPrivacyPolicyVersionShouldRejectBlankValue() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "alice",
                "alice@example.com",
                "Aa123!"
        );
        request.setPrivacyPolicyVersion(" ");

        assertFalse(VALIDATOR.validateProperty(request, "privacyPolicyVersion").isEmpty());
    }
}
