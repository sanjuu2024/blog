package com.ccsanjuu.blog.modules.article.support;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleContentRendererTest {

    private final ArticleContentRenderer articleContentRenderer = new ArticleContentRenderer();

    @Test
    void convertMarkdownToHtmlShouldRenderBasicMarkdownAndTable() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                # Title

                | Name | Value |
                | --- | --- |
                | Java | 21 |
                """);

        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<td>Java</td>"));
    }

    @Test
    void convertMarkdownToHtmlShouldRemoveUnsafeHtml() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                <script>alert(1)</script>
                <img src="javascript:alert(1)" onerror="alert(1)">
                <a href="javascript:alert(1)" onclick="alert(1)">bad link</a>
                <a href="https://example.com" target="_blank">safe link</a>
                """);

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("javascript:"));
        assertFalse(html.contains("onerror"));
        assertFalse(html.contains("onclick"));
        assertTrue(html.contains("href=\"https://example.com\""));
        assertTrue(html.contains("target=\"_blank\""));
        assertTrue(html.contains("rel=\"noopener noreferrer\""));
    }

    @Test
    void convertMarkdownToHtmlShouldPreserveOptionalImageTitlesForFrontendCaptions() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                ![图片替代文本](https://img.example.com/markdown.png "Markdown 图注")

                <img src="https://img.example.com/html.png" alt="HTML 替代文本" title="HTML 图注" width="320" height="180">

                ![只有替代文本](https://img.example.com/plain.png)
                """);
        var images = Jsoup.parseBodyFragment(html).select("img");

        assertEquals(3, images.size());
        assertEquals("Markdown 图注", images.get(0).attr("title"));
        assertEquals("图片替代文本", images.get(0).attr("alt"));
        assertEquals("HTML 图注", images.get(1).attr("title"));
        assertEquals("320", images.get(1).attr("width"));
        assertEquals("180", images.get(1).attr("height"));
        assertFalse(images.get(2).hasAttr("title"));
    }

    @Test
    void convertMarkdownToHtmlShouldPreserveSiteRelativeLinksOnly() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                [留言板](/messages)
                <a href="/message-board">另一个站内路径</a>
                <a href="//external.example/path">协议相对外链</a>
                <a href="///external.example/path">多斜线外链</a>
                """);

        assertTrue(html.contains("href=\"/messages\""));
        assertTrue(html.contains("href=\"/message-board\""));
        assertFalse(html.contains("//external.example/path"));
        assertFalse(html.contains("///external.example/path"));
    }

    @Test
    void convertToTextShouldCollectPlainTextFromMarkdown() {
        String text = articleContentRenderer.convertToText("""
                # Title

                This is **important** content.
                """);

        assertTrue(text.contains("Title"));
        assertTrue(text.contains("important"));
        assertFalse(text.contains("#"));
        assertFalse(text.contains("**"));
    }

    @Test
    void blankMarkdownShouldReturnEmptyContent() {
        assertEquals("", articleContentRenderer.convertMarkdownToHtml(" "));
        assertEquals("", articleContentRenderer.convertToText(null));
    }
}
