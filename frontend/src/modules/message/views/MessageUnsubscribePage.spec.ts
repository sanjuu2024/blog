import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { unsubscribeMessage } from '../api/messageApi';
import MessageUnsubscribePage from './MessageUnsubscribePage.vue';

const routeQuery = vi.hoisted(() => ({ token: '' }));

vi.mock('vue-router', () => ({
	useRoute: () => ({ query: routeQuery }),
}));

vi.mock('../api/messageApi', () => ({
	unsubscribeMessage: vi.fn(),
}));

function mountPage() {
	return mount(MessageUnsubscribePage, {
		global: {
			stubs: {
				ElButton: {
					template: '<button @click="$emit(\'click\')"><slot /></button>',
				},
				RouterLink: {
					template: '<a><slot /></a>',
				},
				ILucideMailMinus: true,
			},
		},
	});
}

describe('MessageUnsubscribePage', () => {
	beforeEach(() => {
		routeQuery.token = '';
		vi.clearAllMocks();
	});

	it('does not allow submitting an invalid token', () => {
		routeQuery.token = 'short-token';
		const wrapper = mountPage();

		expect(wrapper.text()).toContain('退订链接无效或已失效');
		expect(wrapper.find('button').exists()).toBe(false);
	});

	it('submits a valid token only after confirmation', async () => {
		routeQuery.token = 'a'.repeat(20);
		vi.mocked(unsubscribeMessage).mockResolvedValue(null);
		const wrapper = mountPage();

		expect(unsubscribeMessage).not.toHaveBeenCalled();
		await wrapper.get('button').trigger('click');
		await vi.waitFor(() =>
			expect(unsubscribeMessage).toHaveBeenCalledWith({ token: 'a'.repeat(20) }),
		);
		expect(wrapper.text()).toContain('已关闭这条留言的后续通知');
	});
});
