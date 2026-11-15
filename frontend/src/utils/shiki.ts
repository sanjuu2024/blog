import type { HighlighterCore, ShikiTransformer } from 'shiki/core';

const languageLoaders = {
	cpp: () => import('shiki/langs/cpp.mjs'),
	c: () => import('shiki/langs/c.mjs'),
	csharp: () => import('shiki/langs/csharp.mjs'),
	java: () => import('shiki/langs/java.mjs'),
	python: () => import('shiki/langs/python.mjs'),
	javascript: () => import('shiki/langs/javascript.mjs'),
	typescript: () => import('shiki/langs/typescript.mjs'),
	jsx: () => import('shiki/langs/jsx.mjs'),
	tsx: () => import('shiki/langs/tsx.mjs'),
	vue: () => import('shiki/langs/vue.mjs'),
	html: () => import('shiki/langs/html.mjs'),
	xml: () => import('shiki/langs/xml.mjs'),
	css: () => import('shiki/langs/css.mjs'),
	scss: () => import('shiki/langs/scss.mjs'),
	json: () => import('shiki/langs/json.mjs'),
	json5: () => import('shiki/langs/json5.mjs'),
	yaml: () => import('shiki/langs/yaml.mjs'),
	bash: () => import('shiki/langs/bash.mjs'),
	sql: () => import('shiki/langs/sql.mjs'),
	dockerfile: () => import('shiki/langs/dockerfile.mjs'),
	markdown: () => import('shiki/langs/markdown.mjs'),
	http: () => import('shiki/langs/http.mjs'),
};
type CodeLanguage = keyof typeof languageLoaders;

const languageAliases: Record<string, CodeLanguage> = {
	'c++': 'cpp',
	cc: 'cpp',
	cxx: 'cpp',
	hpp: 'cpp',
	cs: 'csharp',
	'c#': 'csharp',
	py: 'python',
	js: 'javascript',
	ts: 'typescript',
	markup: 'html',
	'xml-doc': 'xml',
	yml: 'yaml',
	sh: 'bash',
	shell: 'bash',
	docker: 'dockerfile',
	md: 'markdown',
	clike: 'c',
};

type CodeRenderer = { highlighter: HighlighterCore; brackets: ShikiTransformer };
let rendererPromise: Promise<CodeRenderer> | undefined;
const loadedLanguages = new Map<CodeLanguage, Promise<void>>();
let codeBlockId = 0;

function getRenderer() {
	// 首个有效代码块才加载引擎和主题，之后复用实例；加载失败允许后续正文重试。
	rendererPromise ??= Promise.all([
		import('shiki/core'),
		import('shiki/engine/oniguruma'),
		import('shiki/themes/dark-plus.mjs'),
		import('@shikijs/colorized-brackets'),
	])
		.then(async ([{ createHighlighterCore }, { createOnigurumaEngine }, theme, brackets]) => ({
			highlighter: await createHighlighterCore({
				langs: [],
				themes: [theme.default],
				engine: createOnigurumaEngine(() => import('shiki/wasm')),
			}),
			brackets: brackets.transformerColorizedBrackets(),
		}))
		.catch((error: unknown) => {
			rendererPromise = undefined;
			throw error;
		});
	return rendererPromise;
}

async function loadLanguage(highlighter: HighlighterCore, language: CodeLanguage) {
	if (!loadedLanguages.has(language)) {
		loadedLanguages.set(
			language,
			languageLoaders[language]()
				.then((grammar) => highlighter.loadLanguage(grammar.default))
				.catch((error: unknown) => {
					loadedLanguages.delete(language);
					throw error;
				}),
		);
	}
	await loadedLanguages.get(language);
}

function renderLines(code: HTMLElement, source: string) {
	const lines = source.replace(/\r\n?/g, '\n').split('\n');
	// 围栏代码通常带一个结束换行，保留文本但不额外显示一个空行号。
	if (lines.length > 1 && lines.at(-1) === '') lines.pop();
	// Shiki 和纯文本使用相同的行结构，异步着色前后不改变布局或原始复制内容。
	if (!code.querySelector('.line')) {
		code.replaceChildren();
		lines.forEach((text, index) => {
			const line = document.createElement('span');
			line.className = 'line';
			line.textContent = text;
			code.append(line);
			if (index < lines.length - 1 || source.endsWith('\n')) code.append('\n');
		});
	} else if (source.endsWith('\n') && code.lastElementChild?.textContent === '') {
		code.lastElementChild.remove();
	}
}

type HighlightOptions = {
	signal: AbortSignal;
	copy: (source: string) => Promise<void>;
};

function addToolbar(
	pre: HTMLPreElement,
	source: string,
	language: string,
	options: HighlightOptions,
) {
	const wrapper = document.createElement('div');
	wrapper.className = 'code-toolbar';
	pre.replaceWith(wrapper);
	wrapper.append(pre);
	pre.id ||= `app-code-block-${++codeBlockId}`;
	const toolbar = document.createElement('div');
	toolbar.className = 'toolbar';
	const label = document.createElement('span');
	label.className = 'app-code-language';
	label.textContent = language === 'cpp' ? 'c++' : language;
	label.title = label.textContent;
	const copy = document.createElement('button');
	copy.type = 'button';
	copy.className = 'copy-to-clipboard-button';
	copy.textContent = '复制';
	copy.setAttribute('aria-live', 'polite');
	let resetTimer: ReturnType<typeof setTimeout> | undefined;
	options.signal.addEventListener('abort', () => clearTimeout(resetTimer), { once: true });
	copy.addEventListener(
		'click',
		async () => {
			copy.disabled = true;
			clearTimeout(resetTimer);
			try {
				await options.copy(source);
				if (!options.signal.aborted) copy.textContent = '已复制';
			} catch {
				if (!options.signal.aborted) copy.textContent = '复制失败';
			} finally {
				copy.disabled = false;
				if (!options.signal.aborted) {
					resetTimer = setTimeout(() => (copy.textContent = '复制'), 2000);
				}
			}
		},
		{ signal: options.signal },
	);
	const collapse = document.createElement('button');
	collapse.type = 'button';
	collapse.className = 'app-code-collapse-button';
	collapse.textContent = '收起';
	collapse.setAttribute('aria-expanded', 'true');
	collapse.setAttribute('aria-controls', pre.id);
	collapse.addEventListener(
		'click',
		() => {
			const collapsed = wrapper.classList.toggle('is-code-collapsed');
			collapse.textContent = collapsed ? '展开' : '收起';
			collapse.setAttribute('aria-expanded', String(!collapsed));
		},
		{ signal: options.signal },
	);
	for (const control of [label, copy, collapse]) {
		const item = document.createElement('div');
		item.className = 'toolbar-item';
		item.append(control);
		toolbar.append(item);
	}
	wrapper.append(toolbar);
}

export async function highlightCodeUnder(container: HTMLElement, options: HighlightOptions) {
	if (options.signal.aborted) return;
	const tasks = Array.from(container.querySelectorAll<HTMLElement>('pre > code')).map(
		async (code) => {
			const pre = code.parentElement;
			if (
				!(pre instanceof HTMLPreElement) ||
				pre.parentElement?.classList.contains('code-toolbar')
			)
				return;
			const source = code.textContent ?? '';
			const languageClass = [...code.classList, ...pre.classList].find((name) =>
				name.startsWith('language-'),
			);
			const name = languageClass?.slice('language-'.length).toLowerCase() || 'plaintext';
			const language = Object.hasOwn(languageAliases, name) ? languageAliases[name]! : name;
			pre.classList.add('shiki', `language-${language}`);
			pre.tabIndex = 0;
			code.classList.add(`language-${language}`);
			code.textContent = source;
			renderLines(code, source);
			addToolbar(pre, source, language, options);
			// 超长代码仍可阅读与复制，避免复杂语法在主线程上长时间分析。
			if (!Object.hasOwn(languageLoaders, language) || source.length > 100_000) return;
			try {
				const { highlighter, brackets } = await getRenderer();
				await loadLanguage(highlighter, language as CodeLanguage);
				// 正文替换或组件卸载后，旧语言包加载完成也不能覆盖新正文。
				if (options.signal.aborted || !container.contains(code)) return;
				const template = document.createElement('template');
				template.innerHTML = highlighter.codeToHtml(source, {
					lang: language,
					theme: 'dark-plus',
					transformers: [brackets],
					tokenizeMaxLineLength: 2000,
				});
				// HTML 仅来自 Shiki 对代码文本的转义与着色，不复用正文中的原始 HTML。
				code.replaceChildren(...template.content.querySelector('code')!.childNodes);
				renderLines(code, source);
			} catch {
				// 核心或语言加载失败时保留已生成的纯文本与工具栏，不影响整篇正文。
			}
		},
	);
	await Promise.all(tasks);
}
