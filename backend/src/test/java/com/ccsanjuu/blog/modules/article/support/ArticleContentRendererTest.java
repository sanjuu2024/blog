package com.ccsanjuu.blog.modules.article.support;

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
