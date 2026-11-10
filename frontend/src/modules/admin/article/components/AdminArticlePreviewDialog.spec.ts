import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { highlightCodeUnder } from '@/utils/prism';
import ArticleCatalogSidebar from '@/modules/article/components/ArticleCatalogSidebar.vue';
import AdminArticlePreviewDialog from './AdminArticlePreviewDialog.vue';

vi.mock('@/utils/prism', () => ({ highlightCodeUnder: vi.fn() }));

afterEach(() => vi.restoreAllMocks());

const ElDialog = defineComponent({
	props: ['modelValue', 'fullscreen'],
	template: '<section v-if="modelValue"><slot /></section>',
});

describe('AdminArticlePreviewDialog', () => {
	it('uses real article rendering for formulas, captions and code with snapshot metadata', async () => {
		const wrapper = mount(AdminArticlePreviewDialog, {
			props: {
				modelValue: true,
				loading: false,
				failed: false,
				article: {
					title: '未保存标题',
					summary: '摘要',
					coverUrl: '',
					categoryPath: '技术 / Java',
					tags: [],
					contentHtml:
						'<p>\\(x^2\\)</p><p><img src="https://img.example.com/test.png" title="手动图注"></p><pre><code class="language-java">int a = 1;</code></pre>',
				},
			},
			global: { stubs: { ElDialog } },
		});
		await vi.dynamicImportSettled();
		await flushPromises();
		expect(wrapper.getComponent(ElDialog).props('fullscreen')).not.toBeUndefined();
		expect(wrapper.text()).toContain('未保存标题');
		expect(wrapper.text()).toContain('技术 / Java');
		expect(wrapper.get('.article-image__caption').text()).toBe('手动图注');
		expect(wrapper.find('.katex').exists()).toBe(true);
		expect(highlightCodeUnder).toHaveBeenCalledWith(wrapper.get('.article-markdown').element);
		expect(wrapper.find('.article-like-button').exists()).toBe(false);
		wrapper.unmount();
	});

	it('shows an explicit retry action on failure', async () => {
		const wrapper = mount(AdminArticlePreviewDialog, {
			props: { modelValue: true, loading: false, failed: true, article: null },
			global: {
				stubs: { ElDialog, ElButton: { template: '<button><slot /></button>' } },
			},
		});
		expect(wrapper.get('[role="alert"]').text()).toContain('预览加载失败');
		await wrapper.get('button').trigger('click');
		expect(wrapper.emitted('retry')).toHaveLength(1);
		wrapper.unmount();
	});

	it('targets the real fullscreen dialog for back-to-top and keeps the header close action', async () => {
		const pageScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		const wrapper = mount(AdminArticlePreviewDialog, {
			attachTo: document.body,
			props: {
				modelValue: true,
				loading: false,
				failed: false,
				article: {
					title: '弹窗预览',
					summary: '',
					coverUrl: '',
					categoryPath: '',
					tags: [],
					contentHtml: '<p>长文章正文</p>',
				},
			},
			global: { stubs: { transition: false } },
		});
		await flushPromises();
		const dialog = wrapper.get('.el-dialog').element as HTMLElement;
		const scrollTo = vi.fn();
		dialog.scrollTo = scrollTo;
		await wrapper.get('.app-backtop__button').trigger('click');

		expect(scrollTo).toHaveBeenCalledExactlyOnceWith({ top: 0, behavior: 'smooth' });
		expect(pageScroll).not.toHaveBeenCalled();
		expect(wrapper.get('.el-dialog__header').text()).toContain('实际效果预览');
		await wrapper.get('.admin-article-preview-dialog-header .el-button').trigger('click');
		await vi.waitFor(() =>
			expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([false]),
		);

		await wrapper.setProps({ modelValue: false });
		await wrapper.setProps({ modelValue: true });
		await flushPromises();
		const reopenedDialog = wrapper.get('.el-dialog').element as HTMLElement;
		const reopenedScrollTo = vi.fn();
		reopenedDialog.scrollTo = reopenedScrollTo;
		expect(reopenedDialog).not.toBe(dialog);
		await wrapper.get('.app-backtop__button').trigger('click');
		expect(reopenedScrollTo).toHaveBeenCalledExactlyOnceWith({ top: 0, behavior: 'smooth' });
		expect(scrollTo).toHaveBeenCalledTimes(1);
		wrapper.unmount();
	});

	it('uses the folding catalog inside the dialog and jumps only to preview headings', async () => {
		const pageScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		const editorHeading = document.createElement('h2');
		editorHeading.id = '重复标题';
		editorHeading.textContent = '编辑器中的同名标题';
		document.body.appendChild(editorHeading);
		const editorRect = vi.spyOn(editorHeading, 'getBoundingClientRect');
		const wrapper = mount(AdminArticlePreviewDialog, {
			attachTo: document.body,
			props: {
				modelValue: true,
				loading: false,
				failed: false,
				article: {
					title: '预览标题不进入目录',
					summary: '',
					coverUrl: '',
					categoryPath: '',
					tags: [],
					contentHtml: '<h2>重复标题</h2><h3>子章节</h3><p>正文</p><h2>第二章</h2>',
				},
			},
			global: { stubs: { transition: false } },
		});
		try {
			await flushPromises();
			const dialog = wrapper.get<HTMLElement>('.el-dialog').element;
			const catalog = wrapper.getComponent(ArticleCatalogSidebar);
			expect(catalog.props('teleportTo')).toBe(dialog);
			expect(catalog.props('catalogList').map((item) => item.text)).toEqual([
				'重复标题',
				'子章节',
				'第二章',
			]);
			const sidebar = dialog.querySelector<HTMLElement>('.article-catalog-sidebar')!;
			sidebar.scrollTo = vi.fn();
			const toggle = dialog.querySelector<HTMLButtonElement>(
				'.article-catalog-sidebar-button',
			)!;
			expect(toggle.parentElement).toBe(dialog);
			expect(toggle.getAttribute('aria-expanded')).toBe('false');
			toggle.click();
			await flushPromises();
			expect(toggle.getAttribute('aria-expanded')).toBe('true');
			expect(wrapper.getComponent({ name: 'ElDialog' }).props('closeOnPressEscape')).toBe(
				false,
			);

			const arrow = sidebar.querySelector<HTMLButtonElement>(
				'.article-catalog-sidebar__arrow',
			)!;
			arrow.click();
			await flushPromises();
			expect(sidebar.querySelector('[data-catalog-id="子章节"]')).toBeNull();
			// 打开动画结束事件晚到时，不得重建目录并覆盖已经折叠的分支。
			wrapper.getComponent({ name: 'ElDialog' }).vm.$emit('opened');
			await flushPromises();
			expect(sidebar.querySelector('[data-catalog-id="子章节"]')).toBeNull();
			expect(toggle.getAttribute('aria-expanded')).toBe('true');
			arrow.click();
			await flushPromises();
			expect(sidebar.querySelector('[data-catalog-id="子章节"]')).not.toBeNull();

			const heading = wrapper.get<HTMLElement>('.article-markdown h2').element;
			Object.defineProperty(dialog, 'scrollTop', { configurable: true, value: 100 });
			vi.spyOn(dialog, 'getBoundingClientRect').mockReturnValue({ top: 0 } as DOMRect);
			vi.spyOn(heading, 'getBoundingClientRect').mockReturnValue({ top: 300 } as DOMRect);
			const scrollTo = vi.fn();
			dialog.scrollTo = scrollTo;
			sidebar.querySelector<HTMLButtonElement>('[data-catalog-id="重复标题"]')!.click();
			await flushPromises();
			expect(scrollTo).toHaveBeenCalledExactlyOnceWith({
				top: 400 - (catalog.props('topOffset')! + 16),
				behavior: 'smooth',
			});
			expect(pageScroll).not.toHaveBeenCalled();
			expect(editorRect).not.toHaveBeenCalled();
			expect(wrapper.find('.article-like-button').exists()).toBe(false);
			expect(wrapper.find('.article-comment-button').exists()).toBe(false);

			await wrapper.get('.article-markdown p').trigger('click');
			expect(toggle.getAttribute('aria-expanded')).toBe('false');
		} finally {
			wrapper.unmount();
			editorHeading.remove();
		}
	});

	it('resets and removes the catalog on closing, reopening, loading, failure and heading-free content', async () => {
		const article = {
			title: '目录预览',
			summary: '',
			coverUrl: '',
			categoryPath: '',
			tags: [],
			contentHtml: '<h2>旧章节</h2>',
		};
		const wrapper = mount(AdminArticlePreviewDialog, {
			attachTo: document.body,
			props: { modelValue: true, loading: false, failed: false, article },
			global: { stubs: { transition: false } },
		});
		try {
			await flushPromises();
			let catalog = wrapper.getComponent(ArticleCatalogSidebar);
			wrapper
				.get<HTMLElement>('.el-dialog')
				.element.querySelector<HTMLElement>('.article-catalog-sidebar')!.scrollTo = vi.fn();
			await catalog.getComponent({ name: 'AppFixedButton' }).trigger('click');
			expect(catalog.props('expanded')).toBe(true);
			await wrapper.setProps({ modelValue: false });
			await flushPromises();
			expect(document.querySelector('.article-catalog-sidebar-button')).toBeNull();
			await wrapper.setProps({ modelValue: true });
			await flushPromises();
			catalog = wrapper.getComponent(ArticleCatalogSidebar);
			wrapper
				.get<HTMLElement>('.el-dialog')
				.element.querySelector<HTMLElement>('.article-catalog-sidebar')!.scrollTo = vi.fn();
			expect(catalog.props('expanded')).toBe(false);
			await catalog.getComponent({ name: 'AppFixedButton' }).trigger('click');
			await wrapper.setProps({ article: { ...article, contentHtml: '<h3>新章节</h3>' } });
			await flushPromises();
			expect(catalog.props('expanded')).toBe(false);
			expect(catalog.props('catalogList').map((item) => item.text)).toEqual(['新章节']);
			await wrapper.setProps({ loading: true });
			expect(wrapper.findComponent(ArticleCatalogSidebar).exists()).toBe(false);
			await wrapper.setProps({ loading: false, failed: true });
			expect(wrapper.findComponent(ArticleCatalogSidebar).exists()).toBe(false);
			await wrapper.setProps({
				failed: false,
				article: { ...article, contentHtml: '<p>没有标题</p>' },
			});
			await flushPromises();
			expect(wrapper.findComponent(ArticleCatalogSidebar).exists()).toBe(false);
		} finally {
			wrapper.unmount();
		}
	});

	it('handles Escape by closing the catalog before allowing the preview dialog to close', async () => {
		const wrapper = mount(AdminArticlePreviewDialog, {
			attachTo: document.body,
			props: {
				modelValue: true,
				loading: false,
				failed: false,
				article: {
					title: '预览',
					summary: '',
					coverUrl: '',
					categoryPath: '',
					tags: [],
					contentHtml: '<h2>章节</h2>',
				},
			},
			global: { stubs: { transition: false } },
		});
		try {
			await flushPromises();
			const dialog = wrapper.get<HTMLElement>('.el-dialog').element;
			dialog.querySelector<HTMLElement>('.article-catalog-sidebar')!.scrollTo = vi.fn();
			const catalog = wrapper.getComponent(ArticleCatalogSidebar);
			await catalog.getComponent({ name: 'AppFixedButton' }).trigger('click');
			document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape' }));
			await flushPromises();
			expect(catalog.props('expanded')).toBe(false);
			expect(wrapper.emitted('update:modelValue')).toBeUndefined();
			document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape' }));
			await vi.waitFor(() =>
				expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([false]),
			);
		} finally {
			wrapper.unmount();
		}
	});
});
