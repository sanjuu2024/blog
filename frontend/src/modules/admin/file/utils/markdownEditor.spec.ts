import { createRequire } from 'node:module';
import { describe, expect, it } from 'vitest';
import type { GlobalConfig } from 'md-editor-v3';
import { configureEditorMarkdown } from './markdownEditor';

// 使用编辑器实际依赖的解析器验证规则，避免额外安装另一份 Markdown 实现。
const dependencyRequire = createRequire(createRequire(import.meta.url).resolve('md-editor-v3'));
const MarkdownIt = dependencyRequire('markdown-it');

function render(markdown: string) {
	const md: Parameters<NonNullable<GlobalConfig['markdownItConfig']>>[0] = new MarkdownIt({
		html: true,
	});
	configureEditorMarkdown(md, { editorId: 'markdown-regression' });
	const container = document.createElement('div');
	container.innerHTML = md.render(markdown);
	return container;
}

describe('editor Markdown rendering', () => {
	it.each([
		'<img src="https://img.example.com/a.png" alt="图" />',
		'<p><img src="https://img.example.com/a.png" alt="图" /></p>',
	])('parses Markdown after an image block: %s', (image) => {
		const body = render(`${image}\n\n**图片后正文**`);
		expect(body.querySelector('img')?.getAttribute('alt')).toBe('图');
		expect(body.querySelector('strong')?.textContent).toBe('图片后正文');
	});

	it('keeps multiple uploaded images in separate paragraphs before the following Markdown', () => {
		const body = render(
			'<p><img src="/first.png" alt="第一张" /></p>\n<p><img src="/second.png" alt="第二张" /></p>\n\n**图片后正文**',
		);
		const images = Array.from(body.querySelectorAll('p > img:only-child'));
		expect(images.map((image) => image.getAttribute('src'))).toEqual([
			'/first.png',
			'/second.png',
		]);
		expect(images[0]!.parentElement).not.toBe(images[1]!.parentElement);
		expect(body.querySelector('strong')?.textContent).toBe('图片后正文');
	});

	it('renders bold around mark without requiring spaces before Chinese text', () => {
		const body = render(
			'① **<mark>立大志，就是要有崇高的理想信念，牢记使命，自信自励。</mark>**树立正确的政治方向和远大的人生志向（坚持中国特色社会主义，以民族复兴为己任。）',
		);
		expect(body.querySelector('strong > mark')?.textContent).toBe(
			'立大志，就是要有崇高的理想信念，牢记使命，自信自励。',
		);
		expect(body.textContent).not.toContain('**');
		expect(
			render('前文**<mark>重点</mark>**后文').querySelector('strong > mark')?.textContent,
		).toBe('重点');
	});

	it('keeps code, escaped and unmatched delimiters literal', () => {
		const body = render(
			'`**<mark>行内代码</mark>**`\n\n```text\n**<mark>代码块</mark>**\n```\n\n\\*\\*<mark>转义</mark>\\*\\*\n\n**<mark>未闭合</mark>',
		);
		expect(body.querySelector('strong')).toBeNull();
		expect(body.querySelector('p code')?.textContent).toBe('**<mark>行内代码</mark>**');
		expect(body.querySelector('pre code')?.textContent).toBe('**<mark>代码块</mark>**\n');
		expect(body.textContent).toContain('**转义**');
		expect(body.textContent).toContain('**未闭合');
	});
});
