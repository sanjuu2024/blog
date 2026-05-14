export const routes = [
	// 主页面
	{
		path: '/',
		name: 'MainLayout',
		redirect: '/home',
		component: () => import('@/layouts/MainLayout.vue'), // 路由懒加载
		children: [
			{
				path: 'home',
				name: 'Home',
				component: () => import('@/modules/home/views/HomePage.vue'),
			},
			{
				path: 'articles',
				name: 'Article',
				component: () => import('@/modules/article/views/ArticlePage.vue'),
			},
			{
				path: 'categories',
				name: 'Category',
				component: () => import('@/modules/category/views/CategoryPage.vue'),
			},
			{
				path: 'links',
				name: 'Link',
				component: () => import('@/modules/link/views/LinkPage.vue'),
			},
			{
				path: 'about',
				name: 'About',
				component: () => import('@/modules/about/views/AboutPage.vue'),
			},
		],
	},
	// 认证相关页面
	{
		path: '/auth',
		name: 'AuthLayout',
		redirect: '/auth/login',
		component: () => import('@/layouts/AuthLayout.vue'),
		children: [
			{
				path: 'register',
				name: 'Register',
				component: () => import('@/modules/auth/views/RegisterPage.vue'),
				meta: {
					title: '注册',
				},
			},
			{
				path: 'login',
				name: 'Login',
				component: () => import('@/modules/auth/views/LoginPage.vue'),
				meta: {
					title: '登录',
				},
			},
		],
	},
	// 后台页面
	{
		path: '/admin',
		name: 'AdminLayout',
		component: () => import('@/layouts/AdminLayout.vue'),
		meta: {
			requiresAuth: true, // 需要登录
			roles: ['ADMIN'], // 需要 admin 角色
		},
	},
	// 404 NOT FOUND 页面
	{
		path: '/404',
		name: 'NotFound',
		component: () => import('@/views/NotFoundPage.vue'),
		meta: {
			title: '404 NOT FOUND',
		},
	},
	// 任意路由
	{
		path: '/:pathMatch(.*)*',
		name: 'Any',
		redirect: '/404',
	},
];
