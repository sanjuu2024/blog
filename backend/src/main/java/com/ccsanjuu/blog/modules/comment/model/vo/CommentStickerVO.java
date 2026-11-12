package com.ccsanjuu.blog.modules.comment.model.vo;

/**
 * 预置表情的稳定标记、文字名称和同源图片路径。
 */
public record CommentStickerVO(String token, String name, String url) {
}
