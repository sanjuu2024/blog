package com.ccsanjuu.blog.modules.mail.model.bo;

/**
 * 一封回复通知邮件的渲染结果。
 *
 * @param recipient 收件地址
 * @param subject 邮件主题
 * @param plainText 纯文本正文
 * @param html HTML 正文
 */
public record MailMessageContent(
        String recipient,
        String subject,
        String plainText,
        String html
) {
}
