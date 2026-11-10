package com.ccsanjuu.blog.modules.article.support;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.LightInlineParser;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.TextContainer;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;

import java.util.Set;

/**
 * 让配对公式作为原始文本通过 Markdown 解析，交由前台 KaTeX 排版。
 */
final class LatexSourceExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    @Override
    public void parserOptions(MutableDataHolder options) {
    }

    @Override
    public void rendererOptions(MutableDataHolder options) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customInlineParserExtensionFactory(new FormulaParserFactory());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        rendererBuilder.nodeRendererFactory(options -> () -> Set.of(
                // 公式里的 HTML 必须转义，反斜杠与 LaTeX 源保持原样。
                new NodeRenderingHandler<>(Formula.class, (node, context, html) -> html.text(node.getChars()))
        ));
    }

    /**
     * 判断候选分隔符是否被前面的反斜杠转义。
     *
     * @param input 当前段落
     * @param index 候选分隔符位置
     * @return 是否被转义
     */
    private static boolean isEscaped(BasedSequence input, int index) {
        int backslashes = 0;
        for (int cursor = index - 1; cursor >= 0 && input.charAt(cursor) == '\\'; cursor--) {
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    /**
     * 在 Markdown 自身的转义和强调处理前读取完整公式，代码节点仍由原解析器处理。
     *
     * @param inlineParser 当前行内解析器
     * @return 是否消费了完整的配对公式
     */
    private static boolean parseFormula(LightInlineParser inlineParser) {
        BasedSequence input = inlineParser.getInput();
        int start = inlineParser.getIndex();
        String opening;
        String closing;
        if (inlineParser.peek() == '$') {
            opening = inlineParser.peek(1) == '$' ? "$$" : "$";
            closing = opening;
        } else if (inlineParser.peek(1) == '(') {
            opening = "\\(";
            closing = "\\)";
        } else if (inlineParser.peek(1) == '[') {
            opening = "\\[";
            closing = "\\]";
        } else {
            return false;
        }

        int end = input.indexOf(closing, start + opening.length());
        while (end >= 0) {
            boolean partOfDoubleDollar = "$".equals(closing)
                    && ((end > 0 && input.charAt(end - 1) == '$')
                    || (end + 1 < input.length() && input.charAt(end + 1) == '$'));
            if (!isEscaped(input, end) && !partOfDoubleDollar) break;
            end = input.indexOf(closing, end + closing.length());
        }
        if (end < 0) return false;

        end += closing.length();
        inlineParser.flushTextNode();
        inlineParser.appendNode(new Formula(input.subSequence(start, end)));
        inlineParser.setIndex(end);
        return true;
    }

    private static final class FormulaParserFactory implements InlineParserExtensionFactory {

        @Override
        public CharSequence getCharacters() {
            return "\\$";
        }

        @Override
        public Set<Class<?>> getAfterDependents() {
            return Set.of();
        }

        @Override
        public Set<Class<?>> getBeforeDependents() {
            return Set.of();
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }

        @Override
        public InlineParserExtension apply(LightInlineParser lightInlineParser) {
            return new InlineParserExtension() {
                @Override
                public void finalizeDocument(InlineParser inlineParser) {
                }

                @Override
                public void finalizeBlock(InlineParser inlineParser) {
                }

                @Override
                public boolean parse(LightInlineParser inlineParser) {
                    return parseFormula(inlineParser);
                }
            };
        }
    }

    private static final class Formula extends Node implements TextContainer {

        private Formula(BasedSequence chars) {
            super(chars);
        }

        @Override
        public BasedSequence[] getSegments() {
            return new BasedSequence[]{getChars()};
        }

        @Override
        public boolean collectText(ISequenceBuilder<? extends ISequenceBuilder<?, BasedSequence>, BasedSequence> out,
                                   int flags, NodeVisitor nodeVisitor) {
            out.append(getChars());
            return false;
        }
    }
}
