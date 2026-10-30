import { describe, expect, it } from 'vitest';
import { adminRoutes, publicRoutes, routes } from './routes';

describe('P0 route contract', () => {
	it('contains all required public and admin pages', () => {
		const publicNames = publicRoutes.map((route) => route.name);
		const adminNames = adminRoutes.map((route) => route.name);
		const rootNames = routes.map((route) => route.name);
		expect(publicNames).not.toContain('Notifications');
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
				'MessageBoard',
				'MessageUnsubscribe',
			]),
		);
		expect(adminNames).toEqual(
			expect.arrayContaining([
				'AdminUserList',
				'AdminArticle',
				'AdminCategoryList',
				'AdminTagList',
				'AdminFriendLinkList',
				'AdminCommentList',
				'AdminMessageList',
				'AdminAbout',
				'AdminAuditLogList',
			]),
		);
		expect(rootNames).toContain('NotFound');
		expect(authNames).toEqual(expect.arrayContaining(['Login', 'Register']));
		expect(categoryNames).toEqual(
			expect.arrayContaining(['CategoryAllArticles', 'CategoryArticlesPage']),
		);
	});

	it('places the links page between the message board and about page', () => {
		const navigationNames = publicRoutes
			.filter((route) => route.meta?.nav)
			.map((route) => route.name);

		expect(navigationNames.indexOf('Link')).toBe(navigationNames.indexOf('MessageBoard') + 1);
		expect(navigationNames.indexOf('About')).toBe(navigationNames.indexOf('Link') + 1);
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

	it('marks internal-scroll public pages to preserve the full viewport below the header', () => {
		for (const routeName of [
			'Category',
			'MessageBoard',
			'MessageUnsubscribe',
			'CommentUnsubscribe',
		]) {
			const route = publicRoutes.find((candidate) => candidate.name === routeName);
			expect(route?.meta?.fullHeight).toBe(true);
		}
	});
});
