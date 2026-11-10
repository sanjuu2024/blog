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
    void markdownStrikethroughShouldSurviveCleaningAndKeepPlainTextSearchable() {
        String markdown = "普通文字 ~~已删除的 **重点** 内容~~ 后文";
        var body = Jsoup.parseBodyFragment(articleContentRenderer.convertMarkdownToHtml(markdown)).body();

        assertEquals(1, body.select("del").size());
        assertEquals("已删除的 重点 内容", body.selectFirst("del").text());
        assertEquals("重点", body.selectFirst("del strong").text());
        assertEquals("普通文字 已删除的 重点 内容 后文", articleContentRenderer.convertToText(markdown));
    }

    @Test
    void markdownShouldPreserveSmallBigAndMarkWithoutAllowingTheirAttributes() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                <small onclick="alert(1)">小字</small>
                <big style="color:red">大字</big>
                <mark class="custom" onmouseover="alert(1)">重点</mark>
                """);
        var body = Jsoup.parseBodyFragment(html).body();

        assertEquals("小字", body.selectFirst("small").text());
        assertEquals("大字", body.selectFirst("big").text());
        assertEquals("重点", body.selectFirst("mark").text());
        assertTrue(body.select("small[onclick], big[style], mark[class], mark[onmouseover]").isEmpty());
        assertFalse(html.contains("onclick"));
        assertFalse(html.contains("style="));
        assertFalse(html.contains("onmouseover"));
        assertFalse(html.contains("custom"));
    }

    @Test
    void fontColorShouldSurviveCleaningInsideBoldAndUnderline() {
        String markdown = "**<u>③ <font color='red'>xxx观点是xxx首要的观点</font>。</u>**";
        var body = Jsoup.parseBodyFragment(articleContentRenderer.convertMarkdownToHtml(markdown)).body();
        var font = body.selectFirst("strong > u > font");

        assertEquals(1, body.select("strong > u > font").size());
        assertEquals("red", font.attr("color"));
        assertEquals("xxx观点是xxx首要的观点", font.text());
        assertEquals("③ xxx观点是xxx首要的观点。", body.text());
    }

    @Test
    void fontShouldKeepOnlyColorAndLeaveCodeExamplesLiteral() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                <font color="#ff0000" onclick="alert(1)" style="position:fixed" class="custom" size="7" face="Arial">颜色标注</font>

                `<font color='red'>行内代码</font>`

                ```html
                <font color="red">代码块</font>
                ```
                """);
        var body = Jsoup.parseBodyFragment(html).body();
        var fonts = body.select("font");

        assertEquals(1, fonts.size());
        assertEquals("#ff0000", fonts.getFirst().attr("color"));
        assertEquals(1, fonts.getFirst().attributes().size());
        assertEquals("<font color='red'>行内代码</font>", body.selectFirst("p code").text());
        assertEquals("<font color=\"red\">代码块</font>", body.selectFirst("pre code").text());
        assertFalse(html.contains("onclick"));
        assertFalse(html.contains("style="));
        assertFalse(html.contains("custom"));
    }

    @Test
    void doubleEqualsShouldHighlightInlineMarkdownAndPreserveNestedFormatting() {
        String markdown = "正文 ==高亮 **重点**== 后文";
        String html = articleContentRenderer.convertMarkdownToHtml(markdown);
        var body = Jsoup.parseBodyFragment(html).body();

        assertEquals("<mark>高亮 <strong>重点</strong></mark>", body.selectFirst("mark").outerHtml());
        assertEquals("正文 高亮 重点 后文", articleContentRenderer.convertToText(markdown));
    }

    @Test
    void boldAroundMarkShouldRenderWhenFollowedImmediatelyByChineseText() {
        String markdown = "① **<mark>立大志，就是要有崇高的理想信念，牢记使命，自信自励。</mark>**树立正确的政治方向和远大的人生志向（坚持中国特色社会主义，以民族复兴为己任。）";
        var body = Jsoup.parseBodyFragment(articleContentRenderer.convertMarkdownToHtml(markdown)).body();

        assertEquals(1, body.select("strong > mark").size());
        assertEquals("立大志，就是要有崇高的理想信念，牢记使命，自信自励。", body.selectFirst("strong > mark").text());
        assertTrue(body.text().endsWith("树立正确的政治方向和远大的人生志向（坚持中国特色社会主义，以民族复兴为己任。）"));
        assertFalse(body.text().contains("**"));
    }

    @Test
    void boldHtmlBoundariesShouldKeepCodeEscapesAndUnsafeAttributesIsolated() {
        String markdown = """
                前文**<mark onclick="alert(1)">重点</mark>**后文

                **普通加粗**和 *斜体*、**外层 *内层斜体* 粗体**

                `**<mark>行内代码</mark>**`

                \\*\\*转义\\*\\*

                ```text
                **<mark>代码块</mark>**
                ```
                """;
        String html = articleContentRenderer.convertMarkdownToHtml(markdown);
        var body = Jsoup.parseBodyFragment(html).body();

        assertEquals(1, body.select("strong > mark").size());
        assertEquals("重点", body.selectFirst("strong > mark").text());
        assertEquals(3, body.select("strong").size());
        assertEquals(2, body.select("em").size());
        assertFalse(html.contains("onclick"));
        assertEquals("**<mark>行内代码</mark>**", body.selectFirst("p code").text());
        assertEquals("**<mark>代码块</mark>**", body.selectFirst("pre code").text());
        assertTrue(body.text().contains("**转义**"));
    }

    @Test
    void doubleEqualsShouldRemainLiteralWhenEscapedUnmatchedOrInsideCode() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                a == b

                \\==转义==

                ==没有闭合

                `==行内代码==`

                ```text
                ==代码块==
                ```
                """);
        var body = Jsoup.parseBodyFragment(html).body();

        assertTrue(body.select("mark").isEmpty());
        assertTrue(body.text().contains("a == b"));
        assertTrue(body.text().contains("==转义=="));
        assertTrue(body.text().contains("==没有闭合"));
        assertEquals("==行内代码==", body.selectFirst("p code").text());
        assertEquals("==代码块==", body.selectFirst("pre code").text());
    }

    @Test
    void strikethroughShouldKeepCodeLiteralAndRemoveUnsafeHtmlAttributes() {
        String html = articleContentRenderer.convertMarkdownToHtml("""
                `~~行内代码~~`

                ```text
                ~~代码块~~
                ```

                <del onclick="alert(1)" style="color:red">删除内容</del>
                """);
        var body = Jsoup.parseBodyFragment(html).body();

        assertEquals("~~行内代码~~", body.selectFirst("p code").text());
        assertEquals("~~代码块~~", body.selectFirst("pre code").text());
        assertEquals(1, body.select("del").size());
        assertEquals("删除内容", body.selectFirst("del").text());
        assertFalse(html.contains("onclick"));
        assertFalse(html.contains("style="));
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
