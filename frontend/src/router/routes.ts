export const routes = [
	{
		name: 'MainLayout',
		path: '/',
		component: () => import('@/layouts/MainLayout.vue'), // 路由懒加载
	},
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
			},
			{
				name: 'Login',
				path: 'login',
				component: () => import('@/modules/auth/views/LoginPage.vue'),
			},
		],
	},
];
