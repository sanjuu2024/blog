export const routes = [
	// 主页面
	{
		name: 'MainLayout',
		path: '/',
		component: () => import('@/layouts/MainLayout.vue'), // 路由懒加载
	},
	// 认证相关页面
	{
		name: 'AuthLayout',
		path: '/auth',
		redirect: '/auth/login',
		component: () => import('@/layouts/AuthLayout.vue'),
		children: [
			{
				name: 'Register',
				path: 'register',
				component: () => import('@/modules/auth/views/RegisterPage.vue'),
				meta: {
					title: '注册',
				},
			},
			{
				name: 'Login',
				path: 'login',
				component: () => import('@/modules/auth/views/LoginPage.vue'),
				meta: {
					title: '登录',
				},
			},
		],
	},
	// 后台页面
	{
		name: 'AdminLayout',
		path: '/admin',
		component: () => import('@/layouts/AdminLayout.vue'),
		meta: {
			requiresAuth: true, // 需要登录
			roles: ['ADMIN'], // 需要 admin 角色
		},
	},
	// 404 NOT FOUND 页面
	{
		name: 'NotFound',
		path: '/404',
		component: () => import('@/views/NotFoundPage.vue'),
		meta: {
			title: '404 NOT FOUND',
		},
	},
	// 任意路由
	{
		name: 'any',
		path: '/:pathMatch(.*)*',
		redirect: '/404',
	},
];
