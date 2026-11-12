package com.ccsanjuu.blog.modules.comment.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CommentContentLengthValidator.class)
public @interface CommentContentLength {
    String message() default "评论内容长度不能超过 1000 个字符，且表情不能超过 20 个";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
