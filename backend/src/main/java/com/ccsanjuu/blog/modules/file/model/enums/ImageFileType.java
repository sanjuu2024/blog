package com.ccsanjuu.blog.modules.file.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ImageFileType {

    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    GIF("gif", "image/gif"),
    AVIF("avif", "image/avif");

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

    /**
     * 识别 AVIF 的 ISO BMFF 文件类型头，不能把通用 ftyp 当作 AVIF。
     *
     * @param header 文件起始字节，最多读取 4096 字节
     * @return 完整 ftyp 声明 avif 或 avis 品牌时为 true
     */
    public static boolean isAvifHeader(byte[] header) {
        if (header.length < 16 || !"ftyp".equals(new String(header, 4, 4, StandardCharsets.US_ASCII))) {
            return false;
        }
        ByteBuffer bytes = ByteBuffer.wrap(header);
        long boxSize = Integer.toUnsignedLong(bytes.getInt());
        int brandOffset = 8;
        if (boxSize == 1) {
            if (header.length < 24) return false;
            boxSize = bytes.getLong(8);
            brandOffset = 16;
        }
        if (boxSize < brandOffset + 8 || boxSize > header.length || (boxSize - brandOffset) % 4 != 0) {
            return false;
        }
        for (int offset = brandOffset; offset < boxSize; offset += 4) {
            if (offset == brandOffset + 4) continue; // minor_version 不是品牌。
            String brand = new String(header, offset, 4, StandardCharsets.US_ASCII);
            if ("avif".equals(brand) || "avis".equals(brand)) return true;
        }
        return false;
    }
}
