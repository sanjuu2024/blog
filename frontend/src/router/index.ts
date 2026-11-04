import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './routes';
import { prepareArticleListScroll, restoreArticleListScroll } from './articleListScroll';

const router = createRouter({
	history: createWebHistory(),
	routes: routes,
	scrollBehavior(to, from, savedPosition) {
		// 归档切换条件时由页面重置右栏，保留外层页面和左栏导航的位置。
		if (to.name === 'Archive' && from.name === 'Archive') {
			return false;
		}
		// 列表筛选通过 query 导航，保持当前页面位置；跨页面导航仍回到顶部。
		if (to.name === 'ArticleList' && from.name === 'ArticleList' && to.hash === from.hash) {
			return false;
		}
		if (to.name === 'ArticleList') {
			return restoreArticleListScroll(to.fullPath, savedPosition ?? { left: 0, top: 0 });
		}

		return {
			left: 0,
			top: 0,
		};
	},
});

router.beforeResolve((to, from) => {
	prepareArticleListScroll(
		to.name === 'ArticleList' && (from.name !== 'ArticleList' || to.hash !== from.hash),
	);
});

router.afterEach((_to, _from, failure) => {
	if (failure) prepareArticleListScroll(false);
});

export default router;
