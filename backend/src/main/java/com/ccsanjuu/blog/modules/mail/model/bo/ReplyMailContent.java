package com.ccsanjuu.blog.modules.mail.model.bo;

/**
 * 回复通知邮件的业务内容。
 *
 * @param recipient 收件地址
 * @param subject 邮件主题
 * @param intro 开场说明
 * @param contextLabel 上下文标题，为空时不展示
 * @param context 上下文内容
 * @param originalLabel 原内容标题
 * @param original 原内容
 * @param replyLabel 回复内容标题
 * @param reply 回复内容
 * @param viewLabel 查看链接文案
 * @param viewUrl 查看链接
 * @param unsubscribeUrl 退订链接
 */
public record ReplyMailContent(
        String recipient,
        String subject,
        String intro,
        String contextLabel,
        String context,
        String originalLabel,
        String original,
        String replyLabel,
        String reply,
        String viewLabel,
        String viewUrl,
        String unsubscribeUrl
) {
}
