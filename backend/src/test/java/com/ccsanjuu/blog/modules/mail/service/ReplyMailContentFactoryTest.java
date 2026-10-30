package com.ccsanjuu.blog.modules.mail.service;

import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.bo.ReplyMailContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplyMailContentFactoryTest {

    @Test
    void shouldRenderBrandedCardAndEscapeDynamicHtmlContent() {
        MailMessageContent result = ReplyMailContentFactory.create("青禾边", new ReplyMailContent(
                "reader@example.com",
                "sanjuu",
                "https://blog.example.com",
                "评论收到新的回复",
                "你的评论收到了新的直接回复。",
                "文章",
                "测试文章",
                "你的评论",
                "<原评论>",
                "作者回复",
                "<script>alert(1)</script>",
                "查看回复完整内容",
                "https://blog.example.com/articles/1#article-comments",
                "https://blog.example.com/comments/unsubscribe?token=abc"
        ));

        assertTrue(result.html().contains("#5f9d64"));
        assertTrue(result.html().contains("您在 [青禾边] 的评论有了新的回复！"));
        assertTrue(result.html().contains("sanjuu，"));
        assertTrue(result.html().contains("href=\"https://blog.example.com\""));
        assertTrue(result.html().contains("查看回复完整内容"));
        assertFalse(result.html().contains("display:inline-block"));
        assertTrue(result.html().contains("&lt;原评论&gt;"));
        assertTrue(result.html().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(result.html().contains("<script>"));
        assertTrue(result.html().contains("关闭这条内容的后续通知"));
        assertTrue(result.plainText().contains("<原评论>"));
    }
}
