import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './routes';

export default createRouter({
	history: createWebHistory(),
	routes: routes,
	scrollBehavior(to, from) {
		// 列表筛选通过 query 导航，保持当前页面位置；跨页面导航仍回到顶部。
		if (to.name === 'ArticleList' && from.name === 'ArticleList' && to.hash === from.hash) {
			return false;
		}

		return {
			left: 0,
			top: 0,
		};
	},
});
