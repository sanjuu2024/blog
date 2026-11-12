package com.ccsanjuu.blog.common.validation;

import com.ccsanjuu.blog.modules.comment.service.CommentStickerService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StickerContentLengthValidator implements ConstraintValidator<StickerContentLength, String> {

    private final CommentStickerService commentStickerService;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || (commentStickerService.countLength(value.trim()) <= 1000
                && commentStickerService.countStickers(value) <= 20);
    }
}
