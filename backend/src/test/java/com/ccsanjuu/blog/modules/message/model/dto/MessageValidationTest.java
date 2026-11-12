package com.ccsanjuu.blog.modules.message.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.ccsanjuu.blog.modules.comment.service.impl.CommentStickerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageValidationTest {

    private static final Validator VALIDATOR = createValidator();

    private static Validator createValidator() {
        try {
            var beans = new DefaultListableBeanFactory();
            beans.registerSingleton("commentStickerService", new CommentStickerServiceImpl(new ObjectMapper()));
            return Validation.byDefaultProvider().configure()
                    .constraintValidatorFactory(new SpringConstraintValidatorFactory(beans))
                    .buildValidatorFactory().getValidator();
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @Test
    void messageAndReplyShouldCountKnownStickersAndEnforceAllLimits() {
        String token = "[sticker:bilibili:doge]";
        for (String content : List.of("字".repeat(980) + token.repeat(20), "[sticker:unknown:doge]", "字".repeat(1000))) {
            assertTrue(VALIDATOR.validate(CreateMessageRequestDTO.builder().content(content).build()).isEmpty());
            CreateMessageReplyRequestDTO reply = new CreateMessageReplyRequestDTO();
            reply.setContent(content);
            assertTrue(VALIDATOR.validate(reply).isEmpty());
        }
        for (String content : List.of(token.repeat(21), "字".repeat(1000) + token, " ".repeat(2260) + token)) {
            assertFalse(VALIDATOR.validate(CreateMessageRequestDTO.builder().content(content).build()).isEmpty());
            CreateMessageReplyRequestDTO reply = new CreateMessageReplyRequestDTO();
            reply.setContent(content);
            assertFalse(VALIDATOR.validate(reply).isEmpty());
        }
    }

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
    void createMessageRequestShouldRejectOversizedTurnstileToken() {
        CreateMessageRequestDTO request = CreateMessageRequestDTO.builder()
                .content("留言内容")
                .turnstileToken("a".repeat(2049))
                .build();

        assertFalse(VALIDATOR.validate(request).isEmpty());
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
