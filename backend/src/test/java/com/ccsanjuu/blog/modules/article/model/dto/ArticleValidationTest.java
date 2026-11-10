package com.ccsanjuu.blog.modules.article.model.dto;

import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void articleUpsertRequestShouldAcceptValidRequest() {
        ArticleUpsertRequestDTO request = validUpsertRequest();

        assertTrue(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void articleUpsertRequestShouldRejectBlankRequiredFields() {
        ArticleUpsertRequestDTO request = validUpsertRequest();
        request.setTitle("");
        request.setContentMd(" ");
        request.setCategoryId(null);
        request.setStatus(null);

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void articleUpsertRequestShouldRejectInvalidLengthsAndIds() {
        ArticleUpsertRequestDTO request = validUpsertRequest();
        request.setTitle("a".repeat(201));
        request.setSummary("a".repeat(501));
        request.setCoverUrl("a".repeat(501));
        request.setCategoryId(0L);
        request.setTagIds(List.of(1L, 0L));

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void adminArticleQueryShouldValidatePageAndFilterFields() {
        AdminArticleQueryDTO validRequest = new AdminArticleQueryDTO();
        validRequest.setPageNum(1);
        validRequest.setPageSize(100);
        validRequest.setCategoryId(20001L);

        AdminArticleQueryDTO invalidRequest = new AdminArticleQueryDTO();
        invalidRequest.setPageNum(0);
        invalidRequest.setPageSize(101);
        invalidRequest.setCategoryId(0L);
        invalidRequest.setTitle("a".repeat(201));

        assertTrue(VALIDATOR.validate(validRequest).isEmpty());
        assertFalse(VALIDATOR.validate(invalidRequest).isEmpty());
    }

    @Test
    void publicArticleQueryShouldValidatePageAndFilterFields() {
        PublicArticleQueryDTO validRequest = new PublicArticleQueryDTO();
        validRequest.setPageNum(1);
        validRequest.setPageSize(20);
        validRequest.setKeyword("Spring Boot");
        validRequest.setCategoryId(20001L);
        validRequest.setTagIds(List.of(30001L, 30002L));

        PublicArticleQueryDTO invalidRequest = new PublicArticleQueryDTO();
        invalidRequest.setPageNum(0);
        invalidRequest.setPageSize(21);
        invalidRequest.setKeyword("a".repeat(101));
        invalidRequest.setCategoryId(0L);
        invalidRequest.setTagIds(List.of(30001L, 0L));

        assertTrue(VALIDATOR.validate(validRequest).isEmpty());
        assertFalse(VALIDATOR.validate(invalidRequest).isEmpty());
    }

    private ArticleUpsertRequestDTO validUpsertRequest() {
        return ArticleUpsertRequestDTO.builder()
                .title("Spring Boot notes")
                .summary("A short summary")
                .contentMd("# Hello")
                .categoryId(21001L)
                .tagIds(List.of(30001L))
                .coverUrl("https://example.com/cover.png")
                .isTop(false)
                .status(ArticleStatus.DRAFT)
                .allowComment(true)
                .build();
    }
}
