import { Blob } from 'node:buffer';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { highlightCodeUnder } from './shiki';

const controllers: AbortController[] = [];

// jsdom 的 Blob 缺少 stream，WASM 解压使用 Node 的完整 Blob 实现。
beforeAll(() => vi.stubGlobal('Blob', Blob));
afterAll(() => vi.unstubAllGlobals());

function render(source: string, language?: string) {
	const container = document.createElement('div');
	const pre = document.createElement('pre');
	const code = document.createElement('code');
	code.textContent = source;
	if (language) code.className = `language-${language}`;
	pre.append(code);
	container.append(pre);
	document.body.append(container);
	const controller = new AbortController();
	controllers.push(controller);
	const copy = vi.fn<(text: string) => Promise<void>>().mockResolvedValue();
	const options = { signal: controller.signal, copy };
	const ready = highlightCodeUnder(container, options);
	return { container, pre, code, controller, copy, options, ready };
}

afterEach(() => {
	controllers.splice(0).forEach((controller) => controller.abort());
	document.body.replaceChildren();
});

describe('Shiki article code rendering', () => {
	it('colors C++ syntax and nested bracket pairs without changing source, blank lines or tabs', async () => {
		const source =
			'bool test(vector<int>& nums) {\n\tif (nums[0]) {\n\n\t\treturn true;\n\t}\n}\n';
		const { container, code, ready } = render(source, 'cpp');
		await ready;
		expect(code.textContent).toBe(source);
		expect(code.querySelectorAll(':scope > .line')).toHaveLength(6);
		expect(container.querySelector('.app-code-language')?.textContent).toBe('c++');
		const tokens = Array.from(code.querySelectorAll<HTMLElement>('.line > span'));
		expect(tokens.find((token) => token.textContent === 'bool')?.style.color).toBeTruthy();
		const brackets = tokens.filter(
			(token) => token.textContent === '{' || token.textContent === '}',
		);
		expect(brackets).toHaveLength(4);
		expect(brackets[0]!.style.color).toBe(brackets[3]!.style.color);
		expect(brackets[1]!.style.color).toBe(brackets[2]!.style.color);
		expect(brackets[0]!.style.color).not.toBe(brackets[1]!.style.color);
	});

	it('leaves strings and comments out of bracket matching and keeps HTML examples literal', async () => {
		const source =
			'const example = "<img src=x onerror=alert(1)> {";\n// }\nif (true) { call([1]); }\n';
		const { container, code, ready } = render(source, 'js');
		await ready;
		expect(code.textContent).toBe(source);
		expect(container.querySelector('img, script, [onerror]')).toBeNull();
		const tokens = Array.from(code.querySelectorAll<HTMLElement>('.line > span'));
		const braces = tokens.filter(
			(token) => token.textContent === '{' || token.textContent === '}',
		);
		expect(braces).toHaveLength(2);
		expect(braces[0]!.style.color).toBe(braces[1]!.style.color);
	});

	it.each(['ts', 'vue'])(
		'matches generic brackets next to an array type in %s',
		async (language) => {
			const statement = 'let userList = ref<UserData[]>([]);';
			const source =
				language === 'vue'
					? `<script setup lang="ts">\n${statement}\n</script>`
					: statement;
			const { code, ready } = render(source, language);
			await ready;
			expect(code.textContent).toBe(source);
			const line = code.querySelectorAll('.line')[language === 'vue' ? 1 : 0]!;
			const brackets = Array.from(line.querySelectorAll<HTMLElement>('span')).filter(
				(token) => token.textContent === '<' || token.textContent === '>',
			);
			expect(brackets).toHaveLength(2);
			expect(brackets[0]!.style.color).toBe('rgb(255, 215, 0)');
			expect(brackets[1]!.style.color).toBe(brackets[0]!.style.color);
		},
	);

	it('matches consecutive nested generic closers and resets nesting before the call arguments', async () => {
		const source = 'const value = ref<Map<string, UserData[]>>(new Map());';
		const { code, ready } = render(source, 'ts');
		await ready;
		const tokens = Array.from(code.querySelectorAll<HTMLElement>('.line > span'));
		const angles = tokens.filter(
			(token) => token.textContent === '<' || token.textContent === '>',
		);
		expect(angles).toHaveLength(4);
		expect(angles[0]!.style.color).toBe('rgb(255, 215, 0)');
		expect(angles[1]!.style.color).toBe('rgb(218, 112, 214)');
		expect(angles[2]!.style.color).toBe(angles[1]!.style.color);
		expect(angles[3]!.style.color).toBe(angles[0]!.style.color);
		expect(tokens.find((token) => token.textContent === '(')?.style.color).toBe(
			angles[0]!.style.color,
		);
		expect(code.textContent).toBe(source);
	});

	it('keeps comparisons, strings and comments out of generic bracket matching', async () => {
		const source =
			'const html = "<UserData[]>";\n// <UserData[]>\nconst ok = left < right && right > 0;';
		const { code, ready } = render(source, 'ts');
		await ready;
		const comparisons = Array.from(code.querySelectorAll<HTMLElement>('.line > span')).filter(
			(token) => token.textContent === '<' || token.textContent === '>',
		);
		expect(comparisons).toHaveLength(2);
		expect(comparisons.map((token) => token.style.color)).toEqual([
			'rgb(212, 212, 212)',
			'rgb(212, 212, 212)',
		]);
		expect(code.textContent).toBe(source);
	});

	it('still marks an actually unmatched opening parenthesis as an error', async () => {
		const { code, ready } = render('const value = ref<UserData[]>([];', 'ts');
		await ready;
		const opener = Array.from(code.querySelectorAll<HTMLElement>('.line > span')).find(
			(token) => token.textContent === '(',
		);
		expect(opener?.style.color).toBe('rgba(255, 18, 18, 0.8)');
	});

	it.each([
		['c', 'int n = 1;'],
		['c++', 'bool test() { return true; }'],
		['cs', 'class Example { int n = 1; }'],
		['java', 'class Example { int n = 1; }'],
		['py', 'def test():\n  return True'],
		['ts', 'const n: number = 1;'],
		['jsx', 'const view = <div>Hello</div>;'],
		['tsx', 'const view: JSX.Element = <div>Hello</div>;'],
		['vue', '<template><div>{{ count }}</div></template>'],
		['markup', '<p>Hello</p>'],
		['xml-doc', '<summary>Example</summary>'],
		['css', 'body { color: red; }'],
		['scss', '$color: red; body { color: $color; }'],
		['json', '{"count": 1}'],
		['json5', '{count: 1}'],
		['yml', 'count: 1'],
		['sh', 'echo "Hello"'],
		['sql', 'SELECT id FROM blog_article;'],
		['docker', 'FROM node:22-alpine'],
		['md', '# Hello'],
		['http', 'GET /api/v1/articles HTTP/1.1'],
	])('loads %s and its dependent grammars on demand', async (language, source) => {
		const { code, ready } = render(source, language);
		await ready;
		expect(code.textContent).toBe(source);
		expect(code.querySelector('.line > span[style]')).not.toBeNull();
	});

	it.each([undefined, 'plaintext', 'unknown-language', 'constructor', '__proto__'])(
		'keeps %s code readable and usable as plain text',
		async (language) => {
			const source = '<script>alert(1)</script>\n  text\n';
			const { container, code, copy, ready } = render(source, language);
			await ready;
			expect(code.textContent).toBe(source);
			expect(code.querySelectorAll('.line')).toHaveLength(2);
			expect(container.querySelector('script')).toBeNull();
			container.querySelector<HTMLButtonElement>('.copy-to-clipboard-button')!.click();
			await vi.waitFor(() => expect(copy).toHaveBeenCalledExactlyOnceWith(source));
		},
	);

	it('copies the original code, reports success and failure, and keeps folding local to one block', async () => {
		const source = 'if (ready) {\n    run();\n}\n';
		const first = render(source, 'js');
		const second = render('const other = 1;', 'js');
		await Promise.all([first.ready, second.ready]);
		const copy = first.container.querySelector<HTMLButtonElement>('.copy-to-clipboard-button')!;
		copy.click();
		await vi.waitFor(() => expect(copy.textContent).toBe('已复制'));
		expect(first.copy).toHaveBeenCalledExactlyOnceWith(source);
		first.copy.mockRejectedValueOnce(new Error('Clipboard unavailable'));
		copy.click();
		await vi.waitFor(() => expect(copy.textContent).toBe('复制失败'));
		const collapse = first.container.querySelector<HTMLButtonElement>(
			'.app-code-collapse-button',
		)!;
		collapse.click();
		expect(collapse.getAttribute('aria-expanded')).toBe('false');
		expect(collapse.getAttribute('aria-controls')).toBe(first.pre.id);
		expect(first.container.querySelector('.is-code-collapsed')).not.toBeNull();
		expect(second.container.querySelector('.is-code-collapsed')).toBeNull();
		await highlightCodeUnder(first.container, first.options);
		expect(first.container.querySelectorAll('.code-toolbar')).toHaveLength(1);
		expect(first.container.querySelectorAll('.line')).toHaveLength(3);
		collapse.click();
		expect(collapse.textContent).toBe('收起');
	});

	it('does not apply an asynchronous result after cancellation or replacement of the body', async () => {
		const cancelled = render('const old = 1;', 'js');
		cancelled.controller.abort();
		await cancelled.ready;
		expect(cancelled.code.querySelector('.line > span[style]')).toBeNull();
		const replaced = render('const removed = 1;', 'js');
		replaced.container.innerHTML = '<p>新正文</p>';
		await replaced.ready;
		expect(replaced.container.innerHTML).toBe('<p>新正文</p>');
	});

	it('renders oversized blocks as plain text while retaining exact copy and line content', async () => {
		const source = `const value = "${'x'.repeat(100_000)}";\n`;
		const { code, ready } = render(source, 'js');
		await ready;
		expect(code.textContent).toBe(source);
		expect(code.querySelector('.line > span')).toBeNull();
	});
});
