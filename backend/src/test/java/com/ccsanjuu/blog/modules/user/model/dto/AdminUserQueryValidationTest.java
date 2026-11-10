package com.ccsanjuu.blog.modules.user.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserQueryValidationTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void pageParamsShouldUseSafeRange() {
        UserManagementPageQueryDTO validRequest = new UserManagementPageQueryDTO();
        validRequest.setPageNum(1);
        validRequest.setPageSize(100);

        UserManagementPageQueryDTO invalidRequest = new UserManagementPageQueryDTO();
        invalidRequest.setPageNum(0);
        invalidRequest.setPageSize(101);

        assertTrue(VALIDATOR.validate(validRequest).isEmpty());
        assertFalse(VALIDATOR.validate(invalidRequest).isEmpty());
    }

    @Test
    void usernameAndEmailShouldRejectTooLongValues() {
        UserManagementPageQueryDTO request = new UserManagementPageQueryDTO();
        request.setUsername("a".repeat(21));
        request.setEmail("a".repeat(256));

        assertFalse(VALIDATOR.validateProperty(request, "username").isEmpty());
        assertFalse(VALIDATOR.validateProperty(request, "email").isEmpty());
    }
}
