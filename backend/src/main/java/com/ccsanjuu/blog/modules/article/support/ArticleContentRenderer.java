package com.ccsanjuu.blog.modules.article.support;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ArticleContentRenderer {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    // 清洗白名单列表
    private static final Safelist ARTICLE_HTML_SAFELIST = Safelist.relaxed()
            // Markdown 表格 / 手写安全表格
            .addTags(
                    "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                    "caption", "colgroup", "col"
            )
            .addAttributes("table", "class")
            .addAttributes("thead", "class")
            .addAttributes("tbody", "class")
            .addAttributes("tfoot", "class")
            .addAttributes("tr", "class")
            .addAttributes("th", "class", "colspan", "rowspan", "scope")
            .addAttributes("td", "class", "colspan", "rowspan")
            .addAttributes("caption", "class")

            // 链接
            .addAttributes("a", "href", "title", "target", "rel", "class")
            .addProtocols("a", "href", "http", "https", "mailto")

            // 图片，不建议放开 data:，尤其是 SVG data URI
            .addAttributes("img", "src", "alt", "title", "width", "height", "class")
            .addProtocols("img", "src", "http", "https")

            // 代码高亮常用 class，例如 language-java
            .addAttributes("pre", "class")
            .addAttributes("code", "class")

            // 允许你做一些预设文章样式，比如 tip / warning / note
            .addAttributes("p", "class")
            .addAttributes("span", "class")
            .addAttributes("div", "class")
            .addAttributes("blockquote", "class")
            .addAttributes("ul", "class")
            .addAttributes("ol", "class")
            .addAttributes("li", "class")
            .addAttributes("h1", "class")
            .addAttributes("h2", "class")
            .addAttributes("h3", "class")
            .addAttributes("h4", "class")
            .addAttributes("h5", "class")
            .addAttributes("h6", "class")

            // 如果 a 标签带 target=_blank，强制加 rel，避免 window.opener 风险
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    public ArticleContentRenderer() {
        // 创建配置集
        MutableDataSet options = new MutableDataSet();

        // 添加插件支持
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                TaskListExtension.create(),
                StrikethroughExtension.create()
        ));

        // 创建解析器和渲染器
        this.parser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    /**
     * markdown 转 html
     * @param markdown
     * @return
     */
    public String convertMarkdownToHtml(String markdown) {
        if (!StringUtils.hasText(markdown)){
            return "";
        }

        // 解析 Markdown 文本
        Node document = parser.parse(markdown);

        // 渲染为 HTML
        String rawHtml = htmlRenderer.render(document);

        return Jsoup.clean(rawHtml, ARTICLE_HTML_SAFELIST);
    }

    /**
     * markdown 转 纯文本
     * @param markdown
     * @return
     */
    public String convertToText(String markdown) {
        if (!StringUtils.hasText(markdown)){
            return "";
        }

        Node document = parser.parse(markdown);
        return new TextCollectingVisitor().collectAndGetText(document);
    }
}
