import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getAboutPage } from '../api/aboutApi';
import AboutPage from './AboutPage.vue';

vi.mock('../api/aboutApi', () => ({
	getAboutPage: vi.fn(),
}));

describe('AboutPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		vi.mocked(getAboutPage).mockResolvedValue({
			exists: true,
			contentHtml: '<h1>后端关于页</h1><p>服务端正文</p>',
			contentText: '后端关于页 服务端正文',
			updatedAt: '2026-09-26T00:00:00Z',
		});
	});

	it('renders the about content returned by the backend', async () => {
		const wrapper = mount(AboutPage);
		await flushPromises();

		expect(wrapper.get('.about-content h1').text()).toBe('后端关于页');
		expect(wrapper.get('.about-content p').text()).toBe('服务端正文');
	});

	it('renders the backend placeholder when the about page has not been created', async () => {
		vi.mocked(getAboutPage).mockResolvedValueOnce({
			exists: false,
			contentHtml: '<p>暂无内容</p>',
			contentText: '暂无内容',
			updatedAt: null,
		});
		const wrapper = mount(AboutPage);
		await flushPromises();

		expect(wrapper.get('.about-content').text()).toBe('暂无内容');
	});

	it('renders manually entered title captions without using file names as captions', async () => {
		const contentHtml =
			'<p><img src="/about/cat.png" alt="cat.png" title="手动填写的图注"></p><p><img src="/about/plain.png" alt="plain.png" title=""></p>';
		vi.mocked(getAboutPage).mockResolvedValueOnce({
			exists: true,
			contentHtml,
			contentText: '',
			updatedAt: null,
		});
		const wrapper = mount(AboutPage);
		await flushPromises();

		expect(wrapper.get('.article-image__caption').text()).toBe('手动填写的图注');
		expect(wrapper.findAll('.article-image__caption')).toHaveLength(1);
		expect(wrapper.get('img[src="/about/cat.png"]').attributes('alt')).toBe('cat.png');
		expect(wrapper.find('img[title]').exists()).toBe(false);
		expect(contentHtml).toContain('title="手动填写的图注"');
	});

	it('allows retrying after the about request fails', async () => {
		vi.mocked(getAboutPage).mockRejectedValueOnce(new Error('network failed'));
		const wrapper = mount(AboutPage);
		await flushPromises();

		expect(wrapper.text()).toContain('关于页加载失败');
		await wrapper.get('.about-page__state button').trigger('click');
		await flushPromises();

		expect(wrapper.get('.about-content').text()).toContain('服务端正文');
		expect(getAboutPage).toHaveBeenCalledTimes(2);
	});
});
