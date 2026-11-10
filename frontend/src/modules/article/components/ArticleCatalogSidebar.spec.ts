import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppFixedButton from '@/components/AppFixedButton.vue';
import ArticleCatalogSidebar from './ArticleCatalogSidebar.vue';

afterEach(() => {
	document.body.innerHTML = '';
	vi.restoreAllMocks();
});

const nestedCatalog = [
	{ id: 'section-a', text: '第一章', level: 2 },
	{ id: 'child-a', text: '第一节', level: 3 },
	{ id: 'grandchild-a', text: '小节', level: 4 },
	{ id: 'sibling-a', text: '第二节', level: 3 },
	{ id: 'section-b', text: '第二章', level: 2 },
	{ id: 'child-b', text: '跳级标题', level: 5 },
];

function visibleIds() {
	return Array.from(document.querySelectorAll<HTMLElement>('[data-catalog-id]')).map(
		(element) => element.dataset.catalogId,
	);
}

function titleButton(id: string) {
	return document.querySelector<HTMLButtonElement>(`[data-catalog-id="${id}"]`)!;
}

function branchButton(id: string) {
	return titleButton(id).parentElement!.querySelector<HTMLButtonElement>(
		'.article-catalog-sidebar__arrow',
	)!;
}

function mountNestedCatalog() {
	const wrapper = mount(ArticleCatalogSidebar, {
		props: { expanded: true, catalogList: nestedCatalog },
		global: { stubs: { ILucideList: true, ILucideChevronRight: true } },
	});
	const sidebar = document.querySelector<HTMLElement>('.article-catalog-sidebar')!;
	sidebar.scrollTo = vi.fn();
	return { wrapper, sidebar };
}

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

	it('mounts within a supplied dialog container and accepts its header offset', () => {
		const dialog = document.createElement('div');
		document.body.appendChild(dialog);
		const wrapper = mount(ArticleCatalogSidebar, {
			props: {
				expanded: false,
				catalogList: nestedCatalog,
				teleportTo: dialog,
				topOffset: 72,
			},
		});
		expect(dialog.querySelector('.article-catalog-sidebar-button')).not.toBeNull();
		const sidebar = dialog.querySelector<HTMLElement>('.article-catalog-sidebar')!;
		expect(sidebar.style.getPropertyValue('--article-catalog-top')).toBe('72px');
		wrapper.unmount();
	});

	it('collapses all descendants, keeps siblings, and preserves nested branch choices', async () => {
		const { wrapper } = mountNestedCatalog();
		expect(visibleIds()).toEqual(nestedCatalog.map((item) => item.id));
		expect(branchButton('section-a').getAttribute('aria-expanded')).toBe('true');
		expect(branchButton('section-a').querySelector('.rotate-90')).not.toBeNull();
		expect(branchButton('sibling-a')).toBeNull();

		branchButton('child-a').click();
		await nextTick();
		expect(visibleIds()).not.toContain('grandchild-a');
		branchButton('section-a').click();
		await nextTick();
		expect(visibleIds()).toEqual(['section-a', 'section-b', 'child-b']);
		expect(branchButton('section-a').getAttribute('aria-expanded')).toBe('false');
		expect(branchButton('section-a').getAttribute('aria-label')).toBe('展开第一章的子目录');
		expect(branchButton('section-a').querySelector('.rotate-90')).toBeNull();
		expect(wrapper.emitted('select')).toBeUndefined();
		expect(wrapper.emitted('update:expanded')).toBeUndefined();

		branchButton('section-a').click();
		await nextTick();
		expect(visibleIds()).toEqual(['section-a', 'child-a', 'sibling-a', 'section-b', 'child-b']);
		expect(branchButton('child-a').getAttribute('aria-expanded')).toBe('false');
		wrapper.unmount();
	});

	it('attaches skipped heading levels to the nearest preceding higher heading', async () => {
		const { wrapper } = mountNestedCatalog();
		branchButton('section-b').click();
		await nextTick();
		expect(visibleIds()).toEqual([
			'section-a',
			'child-a',
			'grandchild-a',
			'sibling-a',
			'section-b',
		]);
		expect(titleButton('child-a').parentElement!.style.paddingLeft).toBe('1rem');
		expect(titleButton('grandchild-a').parentElement!.style.paddingLeft).toBe('2rem');
		wrapper.unmount();
	});

	it('keeps a folded branch closed while highlighting its visible ancestor during reading', async () => {
		const { wrapper, sidebar } = mountNestedCatalog();
		const pageScroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
		branchButton('section-a').click();
		await nextTick();
		await wrapper.setProps({ activeCatalogId: 'grandchild-a' });
		await nextTick();

		expect(titleButton('section-a').classList.contains('is-active')).toBe(true);
		expect(visibleIds()).not.toContain('grandchild-a');
		expect(branchButton('section-a').getAttribute('aria-expanded')).toBe('false');
		expect(sidebar.scrollTo).toHaveBeenCalled();
		expect(pageScroll).not.toHaveBeenCalled();
		await wrapper.setProps({ expanded: false });
		await wrapper.setProps({ expanded: true });
		expect(visibleIds()).not.toContain('grandchild-a');
		wrapper.unmount();
	});

	it('keeps title navigation separate from folding and preserves the clicked target highlight', async () => {
		const { wrapper } = mountNestedCatalog();
		titleButton('child-b').click();
		await nextTick();
		expect(wrapper.emitted('select')).toEqual([['child-b']]);
		expect(visibleIds()).toEqual(nestedCatalog.map((item) => item.id));
		await wrapper.setProps({ activeCatalogId: 'child-a' });
		expect(titleButton('child-b').classList.contains('is-active')).toBe(true);
		await wrapper.setProps({ activeCatalogId: 'child-b' });
		await wrapper.setProps({ activeCatalogId: 'section-a' });
		expect(titleButton('section-a').classList.contains('is-active')).toBe(true);
		wrapper.unmount();
	});

	it('resets folding and navigation locks when the article catalog is rebuilt', async () => {
		const { wrapper } = mountNestedCatalog();
		titleButton('child-b').click();
		branchButton('section-a').click();
		await nextTick();
		await wrapper.setProps({
			catalogList: nestedCatalog.map((item) => ({ ...item })),
			activeCatalogId: 'child-a',
		});
		expect(visibleIds()).toEqual(nestedCatalog.map((item) => item.id));
		expect(titleButton('child-a').classList.contains('is-active')).toBe(true);
		wrapper.unmount();
	});
});
