import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { highlightCodeUnder } from '@/utils/prism';
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
});
