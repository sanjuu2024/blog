package com.ccsanjuu.blog.modules.mail.service;

import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.bo.ReplyMailContent;
import org.springframework.web.util.HtmlUtils;

/**
 * 统一生成回复通知的纯文本和带内联样式 HTML 正文。
 */
public final class ReplyMailContentFactory {

    private ReplyMailContentFactory() {
    }

    /**
     * 生成语义一致的纯文本和 HTML 邮件内容。
     *
     * @param content 回复邮件业务内容
     * @return 可直接发送的双格式邮件
     */
    public static MailMessageContent create(String appName, ReplyMailContent content) {
        String contextText = content.contextLabel() == null ? ""
                : content.contextLabel() + "：" + content.context() + "\n";
        String plainText = content.intro() + "\n\n"
                + contextText
                + content.originalLabel() + "：\n" + content.original() + "\n\n"
                + content.replyLabel() + "：\n" + content.reply() + "\n\n"
                + content.viewLabel() + "：" + content.viewUrl() + "\n"
                + "关闭后续通知：" + content.unsubscribeUrl();
        String contextHtml = content.contextLabel() == null ? ""
                : "<p style=\"margin:0 0 16px;color:#475569\"><strong>"
                + escape(content.contextLabel()) + "：</strong>" + escape(content.context()) + "</p>";
        String html = "<div style=\"margin:0 auto;max-width:640px;font-family:Arial,'Microsoft YaHei',sans-serif;color:#1f2937;line-height:1.7\">"
                + "<h2 style=\"margin:0 0 20px;color:#166534;font-size:22px\">" + escape(appName) + "</h2>"
                + "<p style=\"margin:0 0 20px\">" + escape(content.intro()) + "</p>"
                + contextHtml
                + quote(content.originalLabel(), content.original())
                + quote(content.replyLabel(), content.reply())
                + "<p style=\"margin:24px 0\"><a style=\"display:inline-block;padding:10px 18px;border-radius:4px;background:#166534;color:#fff;text-decoration:none\" href=\""
                + escape(content.viewUrl()) + "\">" + escape(content.viewLabel()) + "</a></p>"
                + "<p style=\"margin:0;color:#64748b;font-size:13px\"><a style=\"color:#64748b\" href=\""
                + escape(content.unsubscribeUrl()) + "\">关闭这条内容的后续通知</a></p>"
                + "</div>";
        return new MailMessageContent(content.recipient(), content.subject(), plainText, html);
    }

    private static String quote(String label, String value) {
        return "<div style=\"margin:0 0 16px;padding:12px 16px;border-left:4px solid #86a98c;background:#f6f8f6\">"
                + "<strong>" + escape(label) + "</strong><br>" + escape(value).replace("\n", "<br>") + "</div>";
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
