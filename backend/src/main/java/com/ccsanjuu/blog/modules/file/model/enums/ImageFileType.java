package com.ccsanjuu.blog.modules.file.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ImageFileType {

    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    GIF("gif", "image/gif");

    private final String extension;
    private final String contentType;

    /**
     * 根据 Hutool 识别出的文件类型获取图片格式。
     *
     * @param detectedType 文件真实类型
     * @return 支持的图片格式；不支持时返回 null
     */
    public static ImageFileType fromDetectedType(String detectedType) {
        if ("jpeg".equalsIgnoreCase(detectedType)) {
            return JPEG;
        }
        return Arrays.stream(values())
                .filter(type -> type.extension.equalsIgnoreCase(detectedType))
                .findFirst()
                .orElse(null);
    }
}
