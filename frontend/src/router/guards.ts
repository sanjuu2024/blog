import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';
import { isNavigationFailure, NavigationFailureType, type Router } from 'vue-router';
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

		const isAuthPage = to.path === '/auth/login' || to.path === '/auth/register';

		// 没有 AT
		if (!authStore.accessToken && !authStore.authInitialized) {
			const initialization = authStore.initializeSession();
			if (requiresAuth || isAuthPage) {
				// 需要权限 || 是登陆注册页需要判断是否登录是则不能跳转，所以如果没有登录态就需要阻塞路由等待 AT 刷新看是否还在登录状态
				await initialization;
			} else {
				// 如果不需要权限，先初始化 session，但不阻塞路由跳转；其他相关的后续守卫内容会判断。
				void initialization;
			}
		}

		// 需要权限
		if (requiresAuth) {
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
					ElMessage.warning('请勿重复登录。正在跳转到主页...');
					return '/';
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
	router.afterEach((to, from, failure) => {
		NProgress.done();

		if (import.meta.env.DEV && failure) {
			console.warn('router navigation failure:', {
				to: to.fullPath,
				from: from.fullPath,
				type: failure.type,
				aborted: isNavigationFailure(failure, NavigationFailureType.aborted),
				cancelled: isNavigationFailure(failure, NavigationFailureType.cancelled),
				duplicated: isNavigationFailure(failure, NavigationFailureType.duplicated),
				failure,
			});
		}

		if (to.meta.title) {
			document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog'}`;
		} else {
			document.title = import.meta.env.VITE_APP_TITLE || 'Sanjuu Blog';
		}
	});

	// 3. 全局路由错误日志
	router.onError((error, to, from) => {
		NProgress.done();

		if (import.meta.env.DEV) {
			console.error('router error:', error, { to, from });
		}
	});
}
