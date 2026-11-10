package com.ccsanjuu.blog.modules.mail.service;

import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.bo.ReplyMailContent;
import org.springframework.web.util.HtmlUtils;

/**
 * 统一生成回复通知的纯文本和带内联样式 HTML 正文。
 */
public final class ReplyMailContentFactory {

    private static final String BRAND_COLOR = "#5f9d64";

    private ReplyMailContentFactory() {
    }

    /**
     * 生成语义一致的纯文本和 HTML 邮件内容。
     *
     * @param content 回复邮件业务内容
     * @return 可直接发送的双格式邮件
     */
    public static MailMessageContent create(String appName, ReplyMailContent content) {
        String recipientName = content.recipientName() == null || content.recipientName().isBlank()
                ? "用户" : content.recipientName();
        String plainText = recipientName + "，" + content.intro() + "\n\n"
                + plainQuote(content.originalLabel(), content.original())
                + plainQuote(content.replyLabel(), content.reply())
                + "您可以 " + content.viewLabel() + "：" + content.viewUrl() + "\n"
                + "请注意：此邮件由 " + appName + " 自动发送，请勿直接回复。\n"
                + "若此邮件不是您请求的，请忽略并删除！\n"
                + "关闭这条内容的后续通知：" + content.unsubscribeUrl();
        String contextHtml = content.contextLabel() == null ? ""
                : "<p style=\"margin:0 0 20px;color:#475569;font-size:15px;line-height:1.7\">"
                + escape(content.contextLabel()) + "：<strong style=\"color:" + BRAND_COLOR + "\">"
                + escape(content.context()) + "</strong></p>";
        String html = "<!doctype html><html lang=\"zh-CN\"><body style=\"margin:0;padding:0;background-color:#f4f7f4;"
                + "font-family:Arial,'Microsoft YaHei',sans-serif;color:#374151;line-height:1.7;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:32px 12px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\""
                + " style=\"max-width:640px;background:#fff;border:1px solid #e4e9e4;border-radius:12px;"
                + "box-shadow:0 4px 16px rgba(31,41,55,.10);overflow:hidden;\">"
                + "<tr><td style=\"padding:24px 28px;background-color:" + BRAND_COLOR
                + ";color:#fff;font-size:20px;font-weight:700;line-height:1.5;\">"
                + "您在 [" + escape(appName) + "] 的评论有了新的回复！</td></tr>"
                + "<tr><td style=\"padding:32px 28px 28px;\">"
                + "<p style=\"margin:0 0 20px;color:#475569;font-size:15px;line-height:1.7;\">"
                + escape(recipientName) + "，" + escape(content.intro()) + "</p>"
                + contextHtml
                + quote(content.originalLabel(), content.original())
                + quote(content.replyLabel(), content.reply())
                + "<p style=\"margin:24px 0 16px;color:#475569;font-size:15px;line-height:1.7;\">"
                + "您可以 <a style=\"color:" + BRAND_COLOR + ";text-decoration:underline;\" href=\""
                + escape(content.viewUrl()) + "\">" + escape(content.viewLabel()) + "</a>，"
                + "欢迎再次光临 <a style=\"color:" + BRAND_COLOR + ";text-decoration:underline;\" href=\""
                + escape(content.homeUrl()) + "\">" + escape(appName) + "</a>。</p>"
                + "<p style=\"margin:0 0 12px;color:#6b7280;font-size:13px;line-height:1.7;\">"
                + "此邮件由 " + escape(appName) + " 自动发送，请勿直接回复。</p>"
                + "<p style=\"margin:0 0 12px;color:#6b7280;font-size:13px;line-height:1.7;\">"
                + "若此邮件不是您请求的，请忽略并删除！</p>"
                + "<p style=\"margin:0;color:#6b7280;font-size:13px;line-height:1.7;\"><a style=\"color:"
                + BRAND_COLOR + ";text-decoration:underline;\" href=\"" + escape(content.unsubscribeUrl())
                + "\">关闭这条内容的后续通知</a></p>"
                + "</td></tr></table></td></tr></table></body></html>";
        return new MailMessageContent(content.recipient(), content.subject(), plainText, html);
    }

    private static String quote(String label, String value) {
        String labelHtml = label == null || label.isBlank() ? ""
                : "<p style=\"margin:0 0 8px;color:#6b7280;font-size:15px;line-height:1.7;\">"
                + escape(label) + "</p>";
        return labelHtml + "<div style=\"margin:0 0 20px;padding:16px 18px;background-color:#fff;"
                + "background-image:repeating-linear-gradient(-45deg,#fff 0,#fff 14px,#fafafa 14px,#fafafa 28px);"
                + "border:1px solid #edf1ed;border-radius:8px;color:#4b5563;font-size:15px;line-height:1.8;"
                + "box-shadow:0 2px 8px rgba(31,41,55,.06);\">"
                + escape(value).replace("\n", "<br>") + "</div>";
    }

    private static String plainQuote(String label, String value) {
        return (label == null || label.isBlank() ? "" : label + "\n")
                + (value == null ? "" : value) + "\n\n";
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
