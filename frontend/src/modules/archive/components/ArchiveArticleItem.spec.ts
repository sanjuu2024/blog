import { mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it } from 'vitest';
import ArchiveArticleItem from './ArchiveArticleItem.vue';

const router = createRouter({
	history: createMemoryHistory(),
	routes: [
		{ path: '/articles/:articleId', name: 'ArticleDetail', component: { template: '<div />' } },
		{ path: '/archives', name: 'Archive', component: { template: '<div />' } },
	],
});

describe('ArchiveArticleItem', () => {
	it('shows the local publication date and links to the article and full category path', async () => {
		await router.push('/archives');

		const wrapper = mount(ArchiveArticleItem, {
			global: { plugins: [router] },
			props: {
				article: {
					id: 42,
					title: '归档文章',
					publishedAt: '2025-12-31T16:30:00Z',
					category: {
						id: 22,
						name: 'Java',
						level: 2,
						parent: { id: 11, name: '技术' },
					},
				},
			},
		});

		const date = wrapper.get('time');
		const expectedDate = new Intl.DateTimeFormat('en-CA', {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
		}).format(new Date('2025-12-31T16:30:00Z'));
		expect(date.text()).toBe(expectedDate);
		expect(date.attributes('datetime')).toBe('2025-12-31T16:30:00Z');
		expect(wrapper.get('.archive-article__title').attributes('href')).toBe('/articles/42');

		const categories = wrapper.findAll('.archive-article__category a');
		expect(categories.map((link) => link.text())).toEqual(['技术', 'Java']);
		expect(categories.map((link) => link.attributes('href'))).toEqual([
			'/archives?mode=category&categoryId=11',
			'/archives?mode=category&categoryId=22',
		]);
		wrapper.unmount();
	});
});
