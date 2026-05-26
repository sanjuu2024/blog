package com.ccsanjuu.blog.modules.article.support;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ArticleContentRenderer {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

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
        return htmlRenderer.render(document);
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
