import Prism from 'prismjs';

import 'prismjs/plugins/toolbar/prism-toolbar';
import 'prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard';
import 'prismjs/plugins/line-numbers/prism-line-numbers';

import 'prismjs/components/prism-markup';

import 'prismjs/components/prism-markdown';

import 'prismjs/components/prism-css';
import 'prismjs/components/prism-scss';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-json5';
import 'prismjs/components/prism-http';
import 'prismjs/components/prism-xml-doc';

import 'prismjs/components/prism-clike';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-yaml';
import 'prismjs/components/prism-bash';
import 'prismjs/components/prism-sql';
import 'prismjs/components/prism-docker';

import 'prismjs/components/prism-python';
import 'prismjs/components/prism-c';
import 'prismjs/components/prism-cpp';
import 'prismjs/components/prism-csharp';

// prismjs/components/ 下没有 vue，用 markup 也是差不多的高亮效果
Prism.languages.vue = Prism.languages.markup;

// 代码块工具条
type ToolbarEnv = {
	element: Element;
	language: string;
};

type PrismToolbar = {
	registerButton: (key: string, callback: (env: ToolbarEnv) => HTMLElement | undefined) => void;
};

const toolbar = (Prism.plugins as unknown as { toolbar?: PrismToolbar }).toolbar;

const languageNameMap: Record<string, string> = {
	js: 'javascript',
	javascript: 'javascript',
	ts: 'typescript',
	typescript: 'typescript',
	cpp: 'c++',
	java: 'java',
	python: 'python',
	bash: 'bash',
	sql: 'sql',
	json: 'json',
	vue: 'vue',
};

toolbar?.registerButton('app-language', (env) => {
	const language = languageNameMap[env.language] ?? env.language;
	if (!language) return;

	const span = document.createElement('span');
	span.className = 'app-code-language';
	span.textContent = language;
	return span;
});

toolbar?.registerButton('app-collapse', (env) => {
	const button = document.createElement('button');
	button.type = 'button';
	button.className = 'app-code-collapse-button';
	button.textContent = '收起';
	button.setAttribute('aria-expanded', 'true');

	button.addEventListener('click', () => {
		const pre = env.element.parentElement;
		if (!pre) return;

		const wrapper = pre.parentElement?.classList.contains('code-toolbar')
			? pre.parentElement
			: pre;

		const collapsed = wrapper.classList.toggle('is-code-collapsed');
		button.textContent = collapsed ? '展开' : '收起';
		button.setAttribute('aria-expanded', String(!collapsed));
	});

	return button;
});

export function highlightCodeUnder(container: HTMLElement) {
	// 添加行号类
	container.querySelectorAll('pre').forEach((pre) => {
		pre.classList.add('line-numbers');
	});

	// 兜底，如果代码块没有指定语言，渲染出来的代码块效果会和其他代码块差别很多；兜底把语言都设置为 plaintext
	container.querySelectorAll('pre > code').forEach((code) => {
		const pre = code.parentElement;
		if (!(pre instanceof HTMLPreElement)) return;

		const languageClass =
			Array.from(code.classList).find((className) => className.startsWith('language-')) ||
			Array.from(pre.classList).find((className) => className.startsWith('language-')) ||
			'language-plaintext';

		pre.classList.add('line-numbers', languageClass);
		code.classList.add(languageClass);
	});

	Prism.highlightAllUnder(container);
}
