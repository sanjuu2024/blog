package com.ccsanjuu.blog.modules.comment.service;

import com.ccsanjuu.blog.modules.comment.model.vo.CommentStickerPackVO;

import java.util.List;

public interface CommentStickerService {

    /**
     * 获取只读预置表情目录。
     *
     * @return 表情库及其有序表情列表
     */
    List<CommentStickerPackVO> getPacks();

    /**
     * 计算评论展示长度，已知表情计一，其余沿用 UTF-16 长度。
     *
     * @param content 评论源文本
     * @return 展示长度
     */
    int countLength(String content);

    /**
     * 统计有效表情数量，重复标记逐个计数，未知标记不计入。
     *
     * @param content 评论源文本
     * @return 有效表情数量
     */
    int countStickers(String content);

    /**
     * 将已知表情转换为文字，供后台审核、通知和邮件展示。
     *
     * @param content 评论源文本，可为空
     * @return 保留空值与普通文本的文字内容
     */
    String toPlainText(String content);
}
