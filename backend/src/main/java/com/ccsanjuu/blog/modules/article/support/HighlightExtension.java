package com.ccsanjuu.blog.modules.article.support;

import com.vladsch.flexmark.ext.ins.Ins;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.core.delimiter.Delimiter;
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor;
import com.vladsch.flexmark.parser.delimiter.DelimiterRun;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Set;

/**
 * 将成对的等号分隔符解析为 Markdown 行内高亮。
 */
final class HighlightExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    @Override
    public void parserOptions(MutableDataHolder options) {
    }

    @Override
    public void rendererOptions(MutableDataHolder options) {
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customDelimiterProcessor(new HighlightDelimiterProcessor());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        rendererBuilder.nodeRendererFactory(options -> () -> Set.of(
                new NodeRenderingHandler<>(Highlight.class, (node, context, html) -> {
                    html.withAttr().tag("mark");
                    context.renderChildren(node);
                    html.tag("/mark");
                })
        ));
    }

    private static final class HighlightDelimiterProcessor implements DelimiterProcessor {

        @Override
        public char getOpeningCharacter() {
            return '=';
        }

        @Override
        public char getClosingCharacter() {
            return '=';
        }

        @Override
        public int getMinLength() {
            return 2;
        }

        @Override
        public int getDelimiterUse(DelimiterRun openingRun, DelimiterRun closingRun) {
            return openingRun.length() == 2 && closingRun.length() == 2 ? 2 : 0;
        }

        @Override
        public void process(Delimiter openingDelimiter, Delimiter closingDelimiter, int delimiterUse) {
            Highlight highlight = new Highlight(
                    openingDelimiter.getTailChars(delimiterUse),
                    BasedSequence.NULL,
                    closingDelimiter.getLeadChars(delimiterUse)
            );
            openingDelimiter.moveNodesBetweenDelimitersTo(highlight, closingDelimiter);
        }

        @Override
        public boolean canBeOpener(
                String before,
                String after,
                boolean leftFlanking,
                boolean rightFlanking,
                boolean beforeIsPunctuation,
                boolean beforeIsWhitespace,
                boolean afterIsPunctuation,
                boolean afterIsWhitespace
        ) {
            return leftFlanking;
        }

        @Override
        public boolean canBeCloser(
                String before,
                String after,
                boolean leftFlanking,
                boolean rightFlanking,
                boolean beforeIsPunctuation,
                boolean beforeIsWhitespace,
                boolean afterIsPunctuation,
                boolean afterIsWhitespace
        ) {
            return rightFlanking;
        }

        @Override
        public boolean skipNonOpenerCloser() {
            return false;
        }

        @Override
        public Highlight unmatchedDelimiterNode(InlineParser inlineParser, DelimiterRun delimiterRun) {
            return null;
        }
    }

    private static final class Highlight extends Ins {

        private Highlight(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
            super(openingMarker, text, closingMarker);
        }
    }
}
