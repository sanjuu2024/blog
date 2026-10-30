import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it } from 'vitest';
import AppFixedButton from '@/components/AppFixedButton.vue';
import ArticleCatalogSidebar from './ArticleCatalogSidebar.vue';

afterEach(() => {
	document.body.innerHTML = '';
});

describe('ArticleCatalogSidebar', () => {
	it('keeps the toggle in the body and preserves expansion and Escape handling', async () => {
		const wrapper = mount(ArticleCatalogSidebar, {
			props: {
				expanded: false,
				catalogList: [{ id: 'section-1', text: '第一节', level: 2 }],
			},
			global: { stubs: { ILucideList: true } },
		});
		const fixedButton = wrapper.getComponent(AppFixedButton);
		const button = fixedButton.get('button');

		expect(button.element.parentElement).toBe(document.body);
		expect(fixedButton.props()).toMatchObject({ placement: 'bottom-left', offsetY: 32 });
		expect(button.attributes('aria-expanded')).toBe('false');
		await button.trigger('click');
		expect(wrapper.emitted('update:expanded')).toEqual([[true]]);

		await wrapper.setProps({ expanded: true });
		expect(button.attributes('aria-expanded')).toBe('true');
		expect(button.classes()).toContain('article-catalog-sidebar-button-expanded');
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
		expect(wrapper.emitted('update:expanded')).toEqual([[true], [false]]);
		wrapper.unmount();
	});

	it('does not render a toggle or sidebar when the article has no headings', () => {
		const wrapper = mount(ArticleCatalogSidebar, {
			props: { expanded: false, catalogList: [] },
		});

		expect(wrapper.findComponent(AppFixedButton).exists()).toBe(false);
		expect(document.querySelector('.article-catalog-sidebar')).toBeNull();
		wrapper.unmount();
	});
});
