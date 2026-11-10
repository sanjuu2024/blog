package com.ccsanjuu.blog.modules.about.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AboutPageValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptMaximumLengthMarkdown() {
        assertTrue(VALIDATOR.validate(new UpdateAboutPageRequestDTO("a".repeat(100000))).isEmpty());
    }

    @Test
    void shouldRejectBlankOrOversizedMarkdown() {
        assertFalse(VALIDATOR.validate(new UpdateAboutPageRequestDTO("   ")).isEmpty());
        assertFalse(VALIDATOR.validate(new UpdateAboutPageRequestDTO("a".repeat(100001))).isEmpty());
    }
}
