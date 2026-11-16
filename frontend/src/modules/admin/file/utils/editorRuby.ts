import { allToolbar, type ToolbarNames } from 'md-editor-v3';
import { escape } from 'lodash';

// 保留编辑器默认工具，在上标后插入第一个自定义工具。
export const RUBY_EDITOR_TOOLBARS = (allToolbar as ToolbarNames[]).flatMap<ToolbarNames>(
	(toolbar) => (toolbar === 'sup' ? [toolbar, 0] : [toolbar]),
);

function escapeRubyText(text: string) {
	// HTML 内的文本还会经过 Markdown 解析，实体编码避免星号和公式符号被再次解释。
	return escape(text.trim()).replace(
		/[\\`*_[\]~$=]/g,
		(character) => `&#${character.charCodeAt(0)};`,
	);
}

export function createEditorRubyHtml(original: string, reading: string) {
	return `<ruby>${escapeRubyText(original)}<rp>（</rp><rt>${escapeRubyText(reading)}</rt><rp>）</rp></ruby>`;
}
