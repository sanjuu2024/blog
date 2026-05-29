import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';
import type { Router } from 'vue-router';
import NProgress from 'nprogress'; // 引入进度条
import { useUserStore } from '@/stores/userStore';

NProgress.configure({ showSpinner: false });

export default function setupRouterGuards(router: Router) {
	// 1. 全局前置路由守卫
	router.beforeEach(async (to) => {
		NProgress.start();

		const authStore = useAuthStore();
		// 在当前匹配到的父子路由里，只要有任意一个路由写了 meta.requiresAuth，就认为这个页面需要登录
		const requiresAuth = to.matched.some((route) => route.meta.requiresAuth);
		const requiresAdmin = to.matched.some((route) => route.meta.requiresAdmin);

		// 需要权限
		if (requiresAuth) {
			// 没有 AT
			if (!authStore.accessToken) {
				try {
					if (!authStore.triedRefresh) {
						authStore.triedRefresh = true;
						await authStore.refreshToken(); // 尝试刷新 access token
					}
				} catch {
					// 跳过，后续处理
				}
			}

			if (!authStore.accessToken) {
				ElMessage.error('请先登录。正在跳转到登录页...');
				return {
					path: '/auth/login',
					query: {
						redirect: to.fullPath,
					},
				};
			} else {
				const userStore = useUserStore();

				if (!userStore.userInfo) {
					try {
						await userStore.getCurrentUserProfile();
					} catch {
						authStore.clearAuth();
						ElMessage.error('登录状态已失效。正在跳转到登录页...');
						return {
							path: '/auth/login',
							query: {
								redirect: to.fullPath,
							},
						};
					}
				}

				if (requiresAdmin && userStore.userInfo?.role !== 'ADMIN') {
					ElMessage.error('权限不足，无法访问该页面。正在跳转到主页...');
					return '/';
				}
				return true;
			}
		}

		// 不需要权限
		else {
			// 已登录
			if (authStore.accessToken) {
				if (to.path === '/auth/login' || to.path === '/auth/register') {
					// TODO
					// ElMessage.warning('请勿重复登录。正在跳转到主页...');
					// return '/';

					// 由于目前开发中，退出登录 UI 未实现，暂时不处理
					return true;
				} else {
					return true;
				}
			}
			// 未登录
			else {
				return true;
			}
		}
	});

	// 2. 全局后置路由守卫
	router.afterEach((to) => {
		NProgress.done();
		document.title = to.meta.title || import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog';
	});

	// 3. 全局路由错误日志
	router.onError((error, to, from) => {
		NProgress.done();

		if (import.meta.env.DEV) {
			console.error('router error:', error, { to, from });
		}
	});
}
