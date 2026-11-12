package com.ccsanjuu.blog.modules.comment.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 按原始清单顺序提供的预置表情库。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentStickerPackVO(
        String id,
        String name,
        String iconUrl,
        Double gridMinWidthRem,
        List<CommentStickerVO> items
) {
    public CommentStickerPackVO {
        items = List.copyOf(items);
    }
}
