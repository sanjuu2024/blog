package com.ccsanjuu.blog.modules.file.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminImageUploadScene {

    ARTICLE_COVER("articles/covers"),
    ARTICLE_CONTENT("articles/content"),
    PROJECT_COVER("projects/covers");

    private final String objectKeyPrefix;
}
