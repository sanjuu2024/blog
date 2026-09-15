import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';
import PublicCommentContent from './PublicCommentContent.vue';

const resizeObserver = vi.hoisted(() => ({ callback: undefined as (() => void) | undefined }));

vi.mock('@vueuse/core', () => ({
	useResizeObserver: vi.fn((_target, callback: () => void) => {
		resizeObserver.callback = callback;
	}),
}));

afterEach(() => {
	document.body.innerHTML = '';
});

function mountContent(showActions = false) {
	const commentItem = document.createElement('div');
	commentItem.dataset.commentEntry = '';
	commentItem.scrollIntoView = vi.fn();
	document.body.append(commentItem);

	const wrapper = mount(PublicCommentContent, {
		attachTo: commentItem,
		props: {
			commentId: 60001,
			content: '第一行\n'.repeat(30),
			showActions,
		},
		slots: {
			actions: '<button class="test-comment-action">回复</button>',
		},
		global: {
			stubs: {
				ILucideChevronDown: true,
			},
		},
	});

	return { commentItem, wrapper };
}

describe('PublicCommentContent', () => {
	it('does not show the toggle when the content fits', async () => {
		const { wrapper } = mountContent();
		const content = wrapper.get('.comment-content');
		Object.defineProperty(content.element, 'scrollHeight', { value: 120 });
		Object.defineProperty(content.element, 'clientHeight', { value: 120 });
		resizeObserver.callback?.();
		await nextTick();

		expect(wrapper.find('.comment-content-toggle').exists()).toBe(false);
	});

	it('toggles long content and scrolls the whole comment into view', async () => {
		const { commentItem, wrapper } = mountContent(true);
		const content = wrapper.get('.comment-content');
		Object.defineProperty(content.element, 'scrollHeight', { value: 320 });
		Object.defineProperty(content.element, 'clientHeight', { value: 160 });
		resizeObserver.callback?.();
		await nextTick();

		const toggle = wrapper.get('.comment-content-toggle');
		const toolbar = wrapper.get('.comment-content__bottom-toolbar');
		expect(toggle.text()).toBe('展开');
		expect(toolbar.find('.test-comment-action').exists()).toBe(true);
		expect(toggle.element.parentElement).toBe(toolbar.element);

		await toggle.trigger('click');
		expect(toggle.text()).toBe('收起');
		expect(content.classes()).toContain('comment-content--expanded');
		expect(commentItem.scrollIntoView).toHaveBeenCalledWith({
			behavior: 'smooth',
			block: 'start',
		});

		await toggle.trigger('click');
		expect(toggle.text()).toBe('展开');
		expect(content.classes()).not.toContain('comment-content--expanded');
	});
});
