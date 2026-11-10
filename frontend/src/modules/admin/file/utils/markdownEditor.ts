import { config, type GlobalConfig } from 'md-editor-v3';

export const configureEditorMarkdown: NonNullable<GlobalConfig['markdownItConfig']> = (md) => {
	// 默认强调规则将 HTML 尖括号视为标点，紧接中文时无法识别 mark 外侧的 **。
	// 只补齐 mark 边界上的双星号，仍交给 Markdown 自身配对并跳过代码、转义。
	md.inline.ruler.before('emphasis', 'bold_mark_boundary', (state, silent) => {
		const start = state.pos;
		if (silent || state.src.charAt(start) !== '*') return false;
		const scanned = state.scanDelims(start, true);
		if (scanned.length !== 2) return false;
		const beforeMark = /^<mark(?:\s[^>]*|)>/i.test(state.src.slice(start + 2));
		const afterMark = /<\/mark>$/i.test(state.src.slice(Math.max(0, start - 7), start));
		if (!beforeMark && !afterMark) return false;

		for (let i = 0; i < 2; i++) {
			const token = state.push('text', '', 0);
			token.content = '*';
			state.delimiters.push({
				marker: 0x2a,
				length: 2,
				token: state.tokens.length - 1,
				end: -1,
				open: scanned.can_open || beforeMark,
				close: scanned.can_close || afterMark,
			});
		}
		state.pos += 2;
		return true;
	});
};

config({ markdownItConfig: configureEditorMarkdown });
