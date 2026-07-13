import { describe, expect, it } from 'vitest';
import { adminRoutes, publicRoutes, routes } from './routes';

describe('P0 route contract', () => {
	it('contains all required public and admin pages', () => {
		const publicNames = publicRoutes.map((route) => route.name);
		const adminNames = adminRoutes.map((route) => route.name);
		const rootNames = routes.map((route) => route.name);
		const authLayout = routes.find((route) => route.name === 'AuthLayout');
		const authNames = authLayout?.children?.map((route) => route.name) ?? [];
		const categoryRoute = publicRoutes.find((route) => route.name === 'Category');
		const categoryNames = categoryRoute?.children?.map((route) => route.name) ?? [];

		expect(publicNames).toEqual(
			expect.arrayContaining([
				'Home',
				'ArticleList',
				'ArticleDetail',
				'Category',
				'UserProfile',
			]),
		);
		expect(adminNames).toEqual(
			expect.arrayContaining([
				'AdminUserList',
				'AdminArticle',
				'AdminCategoryList',
				'AdminTagList',
			]),
		);
		expect(rootNames).toContain('NotFound');
		expect(authNames).toEqual(expect.arrayContaining(['Login', 'Register']));
		expect(categoryNames).toEqual(
			expect.arrayContaining(['CategoryAllArticles', 'CategoryArticlesPage']),
		);
	});

	it('does not expose tags as a top navigation entry', () => {
		const navigationRoutes = publicRoutes.filter((route) => route.meta?.nav);

		expect(navigationRoutes.some((route) => route.path.includes('tag'))).toBe(false);
		expect(navigationRoutes.some((route) => route.meta?.title === '标签')).toBe(false);
	});

	it('marks personal and admin pages with required permissions', () => {
		const profileRoute = publicRoutes.find((route) => route.name === 'UserProfile');
		const adminLayout = routes.find((route) => route.name === 'AdminLayout');

		expect(profileRoute?.meta?.requiresAuth).toBe(true);
		expect(adminLayout?.meta?.requiresAuth).toBe(true);
		expect(adminLayout?.meta?.requiresAdmin).toBe(true);
	});
});
