import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import { describe, expect, it, vi } from 'vitest';
import { MESSAGE_STATUS, type PublicMessageItem } from '../types/message';
import MessageListItem from './MessageListItem.vue';

const resizeObserver = vi.hoisted(() => ({ callback: undefined as (() => void) | undefined }));

vi.mock('@vueuse/core', () => ({
	useResizeObserver: vi.fn((_target, callback: () => void) => {
		resizeObserver.callback = callback;
	}),
}));

function message(overrides: Partial<PublicMessageItem> = {}): PublicMessageItem {
	return {
		id: 90001,
		nickname: '访客',
		content: '第一行\n'.repeat(30),
		status: MESSAGE_STATUS.APPROVED,
		moderationReason: null,
		author: null,
		isMine: false,
		replies: [],
		createdAt: '2026-08-12T00:00:00Z',
		...overrides,
	};
}

describe('MessageListItem', () => {
	it('does not show the toggle when the content fits', async () => {
		const wrapper = mount(MessageListItem, {
			props: { item: message() },
			global: {
				stubs: {
					AppUserAvatar: true,
					ElTag: true,
					ILucideTrash2: true,
				},
			},
		});
		const content = wrapper.get('.message-item__content');
		Object.defineProperty(content.element, 'scrollHeight', { value: 120 });
		Object.defineProperty(content.element, 'clientHeight', { value: 120 });
		resizeObserver.callback?.();
		await nextTick();

		expect(wrapper.find('.message-item__content-toggle').exists()).toBe(false);
	});

	it('shows expand and collapse controls only when the content overflows', async () => {
		const wrapper = mount(MessageListItem, {
			props: { item: message({ isMine: true }) },
			global: {
				stubs: {
					AppUserAvatar: true,
					ElTag: true,
					ILucideTrash2: true,
					ILucideChevronDown: true,
				},
			},
		});
		const item = wrapper.get<HTMLElement>('.message-item');
		item.element.scrollIntoView = vi.fn();
		const content = wrapper.get('.message-item__content');
		Object.defineProperty(content.element, 'scrollHeight', { value: 320 });
		Object.defineProperty(content.element, 'clientHeight', { value: 160 });
		resizeObserver.callback?.();
		await nextTick();

		const toggle = wrapper.get('.message-item__content-toggle');
		const toolbar = wrapper.get('.message-reply__bottom-toolbar');
		expect(toggle.text()).toBe('展开');
		expect(toolbar.find('.message-item__delete').exists()).toBe(true);
		expect(toggle.element.parentElement).toBe(toolbar.element);
		expect(content.classes()).not.toContain('message-item__content--expanded');
		expect(toolbar.classes()).not.toContain('message-reply__bottom-toolbar--sticky');

		await toggle.trigger('click');
		expect(toggle.text()).toBe('收起');
		expect(content.classes()).toContain('message-item__content--expanded');
		expect(toolbar.classes()).toContain('message-reply__bottom-toolbar--sticky');
		expect(item.element.scrollIntoView).toHaveBeenCalledWith({
			behavior: 'smooth',
			block: 'start',
		});

		await toggle.trigger('click');
		expect(toggle.text()).toBe('展开');
		expect(content.classes()).not.toContain('message-item__content--expanded');
		expect(toolbar.classes()).not.toContain('message-reply__bottom-toolbar--sticky');
	});
});
