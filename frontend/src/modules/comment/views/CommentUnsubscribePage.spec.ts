import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { unsubscribeComment } from '../api/commentApi';
import CommentUnsubscribePage from './CommentUnsubscribePage.vue';

vi.mock('../api/commentApi', () => ({
	unsubscribeComment: vi.fn(),
}));

const routeState = vi.hoisted(() => ({ token: '' }));

vi.mock('vue-router', () => ({
	useRoute: () => ({ query: { token: routeState.token } }),
}));

describe('CommentUnsubscribePage', () => {
	beforeEach(() => {
		routeState.token = '';
		vi.clearAllMocks();
	});

	it('rejects an invalid token without sending a request', () => {
		const wrapper = mount(CommentUnsubscribePage, {
			global: { stubs: { RouterLink: true, IUnsubscribe: true } },
		});

		expect(wrapper.text()).toContain('退订链接无效或已失效');
		expect(unsubscribeComment).not.toHaveBeenCalled();
	});

	it('submits a valid token and shows the completed state', async () => {
		routeState.token = 'a'.repeat(20);
		vi.mocked(unsubscribeComment).mockResolvedValue(null);
		const wrapper = mount(CommentUnsubscribePage, {
			global: { stubs: { RouterLink: true, IUnsubscribe: true } },
		});

		await wrapper.get('button').trigger('click');

		expect(unsubscribeComment).toHaveBeenCalledWith({ token: 'a'.repeat(20) });
		expect(wrapper.text()).toContain('已关闭这条评论的后续通知');
	});
});
