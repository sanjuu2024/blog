// 后台路由
export const adminRoutes = [
	{
		path: '/admin/dashboard',
		name: 'AdminDashboard',
		component: () => import('@/modules/admin/dashboard/views/AdminDashboardPage.vue'),
		meta: {
			icon: 'dashboard',
			title: '仪表盘',
		},
	},
	{
		path: '/admin/users',
		name: 'AdminUserList',
		component: () => import('@/modules/admin/user/views/AdminUserListPage.vue'),
		meta: {
			icon: 'user',
			title: '用户管理',
		},
	},
	{
		path: '/admin/articles',
		name: 'AdminArticle',
		component: () => import('@/layouts/AdminArticleLayout.vue'),
		redirect: '/admin/articles/list',
		meta: {
			icon: 'article',
			title: '文章管理',
		},
		children: [
			{
				path: '/admin/articles/list',
				name: 'AdminArticleList',
				component: () => import('@/modules/admin/article/views/AdminArticleListPage.vue'),
				meta: {
					title: '文章列表',
					hidden: true, // 在侧边栏隐藏该路由
				},
			},
			{
				path: '/admin/articles/create',
				name: 'AdminArticleCreate',
				component: () => import('@/modules/admin/article/views/AdminArticleEditPage.vue'),
				meta: {
					title: '创建文章',
					hidden: true, // 在侧边栏隐藏该路由
				},
			},
			{
				path: '/admin/articles/:articleId/edit',
				name: 'AdminArticleEdit',
				component: () => import('@/modules/admin/article/views/AdminArticleEditPage.vue'),
				meta: {
					title: '编辑文章',
					hidden: true, // 在侧边栏隐藏该路由
				},
			},
		],
	},
	{
		path: '/admin/categories',
		name: 'AdminCategoryList',
		component: () => import('@/modules/admin/category/views/AdminCategoryListPage.vue'),
		meta: {
			icon: 'category',
			title: '分类管理',
		},
	},
	{
		path: '/admin/tags',
		name: 'AdminTagList',
		component: () => import('@/modules/admin/tag/views/AdminTagListPage.vue'),
		meta: {
			icon: 'tag',
			title: '标签管理',
		},
	},
];

// 前台路由
export const publicRoutes = [
	{
		path: '/home',
		name: 'Home',
		component: () => import('@/modules/home/views/HomePage.vue'),
		meta: {
			title: '首页',
			nav: {
				pos: 'left', // 在顶部导航栏左侧
			},
		},
	},
	{
		path: '/articles',
		name: 'ArticleList',
		component: () => import('@/modules/article/views/ArticleListPage.vue'),
		meta: {
			title: '文章',
			nav: {
				pos: 'left', // 在顶部导航栏左侧
			},
		},
	},
	{
		path: '/articles/:articleId',
		name: 'ArticleDetail',
		component: () => import('@/modules/article/views/ArticleDetailPage.vue'),
		meta: {
			title: '文章详情',
			// hidden: true, // 在顶部导航栏隐藏该路由（只要判断没有 nav.pos 字段就不显示了）
		},
	},
	{
		path: '/categories',
		name: 'Category',
		component: () => import('@/modules/category/views/CategoryPage.vue'),
		meta: {
			title: '分类',
			nav: {
				pos: 'left', // 在顶部导航栏左侧
			},
		},
	},
	{
		path: '/links',
		name: 'Link',
		component: () => import('@/modules/link/views/LinkPage.vue'),
		meta: {
			title: '友链',
			nav: {
				pos: 'right', // 在顶部导航栏右侧
			},
		},
	},
	{
		path: '/about',
		name: 'About',
		component: () => import('@/modules/about/views/AboutPage.vue'),
		meta: {
			title: '关于',
			nav: {
				pos: 'right', // 在顶部导航栏右侧
			},
		},
	},
	{
		path: '/users/me',
		name: 'UserProfile',
		component: () => import('@/modules/user/views/UserProfilePage.vue'),
		meta: {
			title: '个人中心',
			// hidden: true, // 在顶部导航栏隐藏该路由（只要判断没有 nav.pos 字段就不显示了；个人中心路由独立显示）
			requiresAuth: true, // 需要登录
		},
	},
	{
		path: '/users/me/settings',
		name: 'UserSettings',
		component: () => import('@/modules/user/views/UserSettingsPage.vue'),
		meta: {
			title: '个人资料设置',
			// hidden: true, // 在顶部导航栏隐藏该路由（只要判断没有 nav.pos 字段就不显示了）
			requiresAuth: true, // 需要登录
		},
	},
];

// 全部路由
export const routes = [
	// 主页面
	{
		path: '/',
		name: 'MainLayout',
		redirect: '/home',
		component: () => import('@/layouts/MainLayout.vue'), // 路由懒加载
		children: publicRoutes,
	},
	// 认证相关页面
	{
		path: '/auth',
		name: 'AuthLayout',
		redirect: '/auth/login',
		component: () => import('@/layouts/AuthLayout.vue'),
		children: [
			{
				path: '/auth/register',
				name: 'Register',
				component: () => import('@/modules/auth/views/RegisterPage.vue'),
				meta: {
					title: '注册',
				},
			},
			{
				path: '/auth/login',
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
		redirect: '/admin/dashboard',
		component: () => import('@/layouts/AdminLayout.vue'),
		meta: {
			title: '后台管理',
			requiresAuth: true, // 需要登录
			requiresAdmin: true, // 需要管理员权限
		},
		children: adminRoutes,
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
