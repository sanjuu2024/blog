import { useAuthStore } from '@/stores/authStore';
import { ElMessage } from 'element-plus';
import type { Router } from 'vue-router';
import NProgress from 'nprogress'; // 引入进度条

NProgress.configure({ showSpinner: false });

export default function setupRouterGuards(router: Router) {
	// 1. 全局前置路由守卫
	router.beforeEach(async (to) => {
		NProgress.start();

		const authStore = useAuthStore();

		// 需要权限
		if (to.meta.requiresAuth) {
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
				// if (to.meta.roles){
				//     // TODO 需要校验角色
				//     return false;
				// }
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
}
