import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import UserProfilePage from './UserProfilePage.vue';

const mocks = vi.hoisted(() => ({
	getCurrentUserProfile: vi.fn(),
	resizeObserverCallback: undefined as (() => void) | undefined,
}));

vi.mock('../api/userApi', () => ({
	getCurrentUserProfile: mocks.getCurrentUserProfile,
}));

vi.mock('@vueuse/core', () => ({
	useMediaQuery: () => ({ value: false }),
	useResizeObserver: vi.fn((_target, callback: () => void) => {
		mocks.resizeObserverCallback = callback;
	}),
}));

vi.mock('vue-router', () => ({
	useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('@/utils/getRouteIcon', () => ({
	default: () => 'span',
}));

function mountPage() {
	return mount(UserProfilePage, {
		global: {
			stubs: {
				AppUserAvatar: true,
				ElButton: {
					template: '<button><slot name="icon" /><slot /></button>',
				},
				ILucideChessQueen: true,
				ILucideSettings: true,
			},
		},
	});
}

describe('UserProfilePage bio', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		mocks.resizeObserverCallback = undefined;
		mocks.getCurrentUserProfile.mockResolvedValue({
			id: 10001,
			username: 'alice',
			nickname: 'Alice',
			email: 'alice@example.com',
			role: 'USER',
			status: 'ACTIVE',
			avatarUrl: '',
			bio: '这是一段用于测试个人简介折叠状态的文字。'.repeat(5),
			lastLoginAt: null,
			createdAt: '2026-09-19T09:00:00+08:00',
		});
	});

	it('does not show the toggle for a one-pixel rendering difference', async () => {
		const wrapper = mountPage();
		await flushPromises();
		const bio = wrapper.get('.user-profile__bio');
		Object.defineProperty(bio.element, 'scrollHeight', { value: 81 });
		Object.defineProperty(bio.element, 'clientHeight', { value: 80 });

		mocks.resizeObserverCallback?.();
		await nextTick();

		expect(wrapper.find('.user-profile__bio-toggle').exists()).toBe(false);
	});

	it('shows the toggle only for real overflow and expands the bio', async () => {
		const wrapper = mountPage();
		await flushPromises();
		const bio = wrapper.get('.user-profile__bio');
		Object.defineProperty(bio.element, 'scrollHeight', { value: 120 });
		Object.defineProperty(bio.element, 'clientHeight', { value: 80 });

		mocks.resizeObserverCallback?.();
		await nextTick();

		const toggle = wrapper.get('.user-profile__bio-toggle');
		expect(toggle.text()).toBe('展开');
		expect(bio.classes()).not.toContain('user-profile__bio--expanded');

		await toggle.trigger('click');

		expect(toggle.text()).toBe('收起');
		expect(bio.classes()).toContain('user-profile__bio--expanded');

		await toggle.trigger('click');

		expect(toggle.text()).toBe('展开');
		expect(bio.classes()).not.toContain('user-profile__bio--expanded');
	});
});
