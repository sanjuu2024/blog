import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router';
import setupRouterGuards from './guards';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';
import type { CurrentUserInfo } from '@/modules/user/types/user';

const messageMocks = vi.hoisted(() => ({
	error: vi.fn(),
	warning: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: messageMocks,
}));

vi.mock('nprogress', () => ({
	default: {
		configure: vi.fn(),
		start: vi.fn(),
		done: vi.fn(),
	},
}));

const TestPage = { template: '<div />' };
const testRoutes: RouteRecordRaw[] = [
	{ path: '/', name: 'Home', component: TestPage },
	{ path: '/public', name: 'Public', component: TestPage },
	{ path: '/auth/login', name: 'Login', component: TestPage },
	{
		path: '/users/me',
		name: 'Profile',
		component: TestPage,
		meta: { requiresAuth: true },
	},
	{
		path: '/admin',
		name: 'Admin',
		component: TestPage,
		meta: { requiresAuth: true, requiresAdmin: true },
	},
];

function createTestRouter() {
	const router = createRouter({
		history: createMemoryHistory(),
		routes: testRoutes,
	});
	setupRouterGuards(router);
	return router;
}

function createUser(role: CurrentUserInfo['role']): CurrentUserInfo {
	return {
		id: 10001,
		username: 'tester',
		nickname: 'Tester',
		email: 'tester@example.com',
		role,
		status: 'ACTIVE',
		avatarUrl: '',
		bio: '',
		lastLoginAt: null,
		createdAt: '2026-05-01T10:00:00+08:00',
	};
}

describe('router guards', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
		messageMocks.error.mockClear();
		messageMocks.warning.mockClear();
	});

	it('redirects a guest from profile to login with original target', async () => {
		const authStore = useAuthStore();
		authStore.authInitialized = true;
		const router = createTestRouter();

		await router.push('/users/me');

		expect(router.currentRoute.value.path).toBe('/auth/login');
		expect(router.currentRoute.value.query.redirect).toBe('/users/me');
	});

	it('rejects a normal user from admin routes', async () => {
		const authStore = useAuthStore();
		const userStore = useUserStore();
		authStore.accessToken = 'access-token';
		authStore.authInitialized = true;
		userStore.userInfo = createUser('USER');
		const router = createTestRouter();

		await router.push('/admin');

		expect(router.currentRoute.value.path).toBe('/');
		expect(messageMocks.error).toHaveBeenCalledWith(
			'权限不足，无法访问该页面。正在跳转到主页...',
		);
	});

	it('allows an administrator to enter admin routes', async () => {
		const authStore = useAuthStore();
		const userStore = useUserStore();
		authStore.accessToken = 'admin-token';
		authStore.authInitialized = true;
		userStore.userInfo = createUser('ADMIN');
		const router = createTestRouter();

		await router.push('/admin');

		expect(router.currentRoute.value.path).toBe('/admin');
	});

	it('clears invalid session when current profile cannot be loaded', async () => {
		const authStore = useAuthStore();
		const userStore = useUserStore();
		authStore.accessToken = 'invalid-token';
		authStore.authInitialized = true;
		vi.spyOn(userStore, 'getCurrentUserProfile').mockRejectedValue(new Error('unauthorized'));
		const router = createTestRouter();

		await router.push('/users/me');

		expect(router.currentRoute.value.path).toBe('/auth/login');
		expect(authStore.accessToken).toBe('');
	});

	it('redirects an authenticated user away from login', async () => {
		const authStore = useAuthStore();
		authStore.accessToken = 'access-token';
		authStore.authInitialized = true;
		const router = createTestRouter();

		await router.push('/auth/login');

		expect(router.currentRoute.value.path).toBe('/');
		expect(messageMocks.warning).toHaveBeenCalled();
	});
});
