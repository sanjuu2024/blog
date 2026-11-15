import { Blob } from 'node:buffer';
import { flushPromises, mount } from '@vue/test-utils';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import ArticleBody from './ArticleBody.vue';

// jsdom 的 Blob 缺少 stream，WASM 解压使用 Node 的完整 Blob 实现。
beforeAll(() => vi.stubGlobal('Blob', Blob));
afterAll(() => vi.unstubAllGlobals());

describe('ArticleBody with Shiki', () => {
	it('renders syntax alongside formulas, captions and headings without changing surrounding HTML', async () => {
		const wrapper = mount(ArticleBody, {
			props: {
				html: '<h2>章节</h2><p>公式 \\(x^2\\)</p><p><img src="/image.png" title="图注"></p><pre><code class="language-cpp">bool test() { return true; }\n</code></pre>',
			},
		});
		try {
			await vi.dynamicImportSettled();
			await flushPromises();
			expect(wrapper.get('h2').text()).toBe('章节');
			expect(wrapper.find('.katex').exists()).toBe(true);
			expect(wrapper.get('.article-image__caption').text()).toBe('图注');
			expect(wrapper.get('code').element.textContent).toBe('bool test() { return true; }\n');
			await vi.waitFor(() =>
				expect(wrapper.find('code .line > span[style]').exists()).toBe(true),
			);
			expect(wrapper.findAll('.code-toolbar')).toHaveLength(1);
		} finally {
			wrapper.unmount();
		}
	});

	it('replaces code and cancels previous rendering when the body changes', async () => {
		const wrapper = mount(ArticleBody, {
			props: { html: '<pre><code class="language-js">const old = 1;</code></pre>' },
		});
		try {
			await wrapper.setProps({
				html: '<h2>新章节</h2><pre><code class="language-py">print("new")</code></pre>',
			});
			await vi.dynamicImportSettled();
			await flushPromises();
			expect(wrapper.findAll('.code-toolbar')).toHaveLength(1);
			expect(wrapper.get('code').text()).toBe('print("new")');
			expect(wrapper.get('.app-code-language').text()).toBe('python');
			expect(wrapper.text()).not.toContain('const old');
		} finally {
			wrapper.unmount();
		}
	});
});
