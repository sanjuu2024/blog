package com.ccsanjuu.blog.modules.message.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createMessageRequestShouldAcceptValidFields() {
        CreateMessageRequestDTO request = CreateMessageRequestDTO.builder()
                .nickname("访客")
                .email("guest@example.com")
                .content("留言内容")
                .notifyOnReply(true)
                .build();

        assertTrue(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void createMessageRequestShouldOnlyValidateFieldsSharedByAllIdentities() {
        CreateMessageRequestDTO request = CreateMessageRequestDTO.builder()
                .nickname("a".repeat(21))
                .email("not-an-email")
                .content(" ")
                .build();

        assertFalse(VALIDATOR.validate(request).isEmpty());

        request.setContent("留言内容");
        assertTrue(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void messagePageQueryShouldRejectInvalidPageAndIds() {
        MessagePageQueryDTO query = new MessagePageQueryDTO();
        query.setPageNum(0);
        query.setPageSize(101);
        query.setMessageId(0L);
        query.setUserId(-1L);
        query.setGuestNickname("a".repeat(21));
        query.setGuestEmail("a".repeat(256));
        query.setContent("a".repeat(1001));

        assertFalse(VALIDATOR.validate(query).isEmpty());
    }

    @Test
    void unsubscribeRequestShouldRejectBlankShortAndOversizedTokens() {
        MessageUnsubscribeRequestDTO blankRequest = new MessageUnsubscribeRequestDTO();
        blankRequest.setToken(" ");
        MessageUnsubscribeRequestDTO shortRequest = new MessageUnsubscribeRequestDTO();
        shortRequest.setToken("a".repeat(19));
        MessageUnsubscribeRequestDTO oversizedRequest = new MessageUnsubscribeRequestDTO();
        oversizedRequest.setToken("a".repeat(129));

        assertFalse(VALIDATOR.validate(blankRequest).isEmpty());
        assertFalse(VALIDATOR.validate(shortRequest).isEmpty());
        assertFalse(VALIDATOR.validate(oversizedRequest).isEmpty());
    }

    @Test
    void batchApprovalShouldRejectEmptyLists() {
        MessageBatchApprovalRequestDTO emptyRequest = new MessageBatchApprovalRequestDTO();
        emptyRequest.setMessageIds(List.of());

        assertFalse(VALIDATOR.validate(emptyRequest).isEmpty());
    }
}
