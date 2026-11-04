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
    void markdownConversionShouldPreserveLatexDelimitersAndFormulaSource() {
        String markdown = """
                都 1 1 1

                ```cpp
                int a = 1;
                ```

                \\(a*b=2^{14}\\)

                \\[c*d=2^{23}\\]

                \\(a*b*c + x_1_y + \\frac{1}{2}\\)

                $a*b*c + x_1_y$

                $$a*b*c + x_1_y$$

                \\(这里是错误公式，祟拜你）

                $不是

                $asdf$

                $$
                yes！
                $$

                ddddd

                $dsaf但是 测试$
                """;

        var body = Jsoup.parseBodyFragment(articleContentRenderer.convertMarkdownToHtml(markdown)).body();

        assertTrue(body.wholeText().contains("\\(a*b=2^{14}\\)"));
        assertTrue(body.wholeText().contains("\\[c*d=2^{23}\\]"));
        assertTrue(body.wholeText().contains("\\(a*b*c + x_1_y + \\frac{1}{2}\\)"));
        assertTrue(body.wholeText().contains("$a*b*c + x_1_y$"));
        assertTrue(body.wholeText().contains("$$a*b*c + x_1_y$$"));
        assertTrue(body.select("em, strong").isEmpty());
        assertEquals("int a = 1;\n", body.selectFirst("pre code").wholeText());
        assertTrue(body.text().contains("ddddd"));
    }

    @Test
    void formulaParsingShouldLeaveCodeAndNormalMarkdownEscapesUntouched() {
        String markdown = """
                **正常加粗** 和 \\*普通星号\\*

                `\\(a*b\\)` 与 `$x_1$`

                ```text
                \\[c*d\\]
                $$x_1$$
                ```

                [普通链接](https://example.com)

                <img src="https://img.example.com/test.png" alt="图片" title="手动图注">
                """;
        var body = Jsoup.parseBodyFragment(articleContentRenderer.convertMarkdownToHtml(markdown)).body();

        assertEquals("正常加粗", body.selectFirst("strong").text());
        assertTrue(body.text().contains("*普通星号*"));
        assertEquals("\\(a*b\\)", body.select("p code").getFirst().text());
        assertEquals("$x_1$", body.select("p code").get(1).text());
        assertTrue(body.selectFirst("pre code").wholeText().contains("\\[c*d\\]"));
        assertEquals("https://example.com", body.selectFirst("a").attr("href"));
        assertEquals("手动图注", body.selectFirst("img").attr("title"));
    }

    @Test
    void multilineFormulasShouldPreserveLatexCommandsAndRemainSafeText() {
        String markdown = """
                \\[
                \\begin{aligned}
                a_1 &= b \\\\
                c &= d
                \\end{aligned}
                \\]

                \\(\\text{<img src=x onerror=alert(1)> & <script>bad</script>}\\)
                """;
        var body = Jsoup.parseBodyFragment(articleContentRenderer.convertMarkdownToHtml(markdown)).body();

        assertTrue(body.wholeText().contains("\\["));
        assertTrue(body.wholeText().contains("\\begin{aligned}"));
        assertTrue(body.wholeText().contains("a_1 &= b \\\\"));
        assertTrue(body.wholeText().contains("\\end{aligned}"));
        assertTrue(body.select("img, script").isEmpty());
        assertTrue(body.text().contains("<img src=x onerror=alert(1)>"));
        String text = articleContentRenderer.convertToText("公式 \\(x_1^2\\) 和 $a*b*c$ 后文");
        assertTrue(text.contains("\\(x_1^2\\)"));
        assertTrue(text.contains("$a*b*c$"));
    }

    @Test
    void blankMarkdownShouldReturnEmptyContent() {
        assertEquals("", articleContentRenderer.convertMarkdownToHtml(" "));
        assertEquals("", articleContentRenderer.convertToText(null));
    }
}
