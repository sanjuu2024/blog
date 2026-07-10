<template>
	<div
		class="admin-sidebar shadow"
		:class="{ 'is-collapsed': isCollapse }"
	>
		<div class="admin-sidebar__inner">
			<div class="admin-sidebar__header">
				<AppLogo
					v-show="!isCollapse"
					to="/"
					size="30"
				/>
				<el-button
					class="admin-sidebar__collapse-button"
					:aria-label="isCollapse ? '展开菜单' : '折叠菜单'"
					@click="isCollapse = !isCollapse"
				>
					<el-icon>
						<MenuCollapse v-if="isCollapse" />
						<MenuOpen v-else />
					</el-icon>
				</el-button>
			</div>
			<div class="admin-sidebar__nav">
				<!-- 🔺collapse-transition="false"，不加上的话 el 自带的折叠动画会使得文字慢一拍折叠，ui 效果差。 -->
				<el-menu
					:default-active="activeMenuPath"
					class="admin-sidebar__menu"
					background-color="transparent"
					:collapse="isCollapse"
					:collapse-transition="false"
				>
					<!-- 自定义封装 el-menu 递归组件 -->
					<AdminMenu :menuList="adminRoutes"></AdminMenu>
				</el-menu>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { adminRoutes } from '@/router/routes';
import AdminMenu from './AdminMenu.vue';
import MenuCollapse from '~icons/material-symbols/menu-rounded';
import MenuOpen from '~icons/material-symbols/menu-open-rounded';

defineOptions({
	name: 'AdminSidebar',
});

const route = useRoute();

const activeMenuPath = computed(() => {
	// router.matched 已经扁平化处理，是一个包含了当前路由匹配的所有路由记录（每一层单独一个记录）的数组，从父级到子级。
	const activeTopRoute = route.matched.find((matchedRoute) =>
		// 匹配后台管理的一级路由
		adminRoutes.some((adminRoute) => adminRoute.name === matchedRoute.name),
	);

	return activeTopRoute?.path ?? route.path;
});

const props = withDefaults(
	defineProps<{
		collapsed?: boolean;
	}>(),
	{
		collapsed: false,
	},
);

const emit = defineEmits<{
	'update:collapsed': [collapsed: boolean];
}>();

// 控制侧边栏展开 / 折叠，状态由 AdminLayout 持有，这样 header/main 的宽度也能同步变化。
const isCollapse = computed({
	get: () => props.collapsed,
	set: (value) => emit('update:collapsed', value),
});
</script>

<style scoped lang="scss">
.admin-sidebar {
	position: absolute;
	left: 0;
	top: 0;
	width: var(--app-admin-sidebar-width);
	height: 100dvh;
	background-color: var(--app-surface);
	overflow: hidden;
	transition: width 0.2s ease;
	z-index: 10;
}

.admin-sidebar__inner {
	padding-inline: 0.5rem;
	padding-block: 1rem;
	transition: padding 0.2s ease;
	// 使用 flex 布局，限制高度
	display: flex;
	flex-direction: column;
	height: 100dvh;
}

.admin-sidebar__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 2rem;
	margin-bottom: 1rem;
}

.admin-sidebar__nav {
	// 接管剩余高度（固定），可使用滚动条
	flex: 1;
	overflow: auto;
	&::-webkit-scrollbar {
		width: 8px;
	}
	&::-webkit-scrollbar-thumb {
		border-radius: 999px;
	}
	&::-webkit-scrollbar-thumb:hover {
		// background: #DEF2E5;
		background: lightgray;
	}
	&::-webkit-scrollbar-track {
		background: transparent;
	}
}

.admin-sidebar__menu {
	border-right: none;
	font-weight: 520;
	width: 100%;
	--el-menu-item-font-size: 1rem;

	:deep(.el-menu-item) {
		border-radius: 0.25rem;
	}

	:deep(.el-menu-item:hover) {
		color: var(--el-color-primary);
		background-color: var(--app-router-hover);
	}

	:deep(.el-menu-item.is-active) {
		color: var(--el-color-primary);
		background-color: var(--app-router-hover);
		font-weight: 600;
	}
}

.admin-sidebar__collapse-button {
	width: 2.25rem;
	height: 2.25rem;
	padding: 0;
	border: none;
	background: transparent;
	color: var(--app-text);
	font-size: 1.5rem;
}

.admin-sidebar__collapse-button:hover {
	background: var(--el-fill-color-light);
}

.admin-sidebar__collapse-button:focus:not(:hover) {
	background: transparent;
	box-shadow: none;
}

.admin-sidebar__collapse-button:focus-visible {
	outline: 2px solid var(--el-color-primary-light-5);
	outline-offset: 2px;
}

.admin-sidebar.is-collapsed {
	.admin-sidebar__header {
		justify-content: center;
	}

	.admin-sidebar__menu {
		--el-menu-base-level-padding: 12px;
		--el-menu-icon-width: 24px;

		:deep(.el-menu-item),
		:deep(.el-sub-menu__title),
		:deep(.el-menu-tooltip__trigger) {
			justify-content: center;
			padding-inline: 0;
		}

		:deep(.el-menu-item .el-icon),
		:deep(.el-sub-menu__title .el-icon),
		:deep(.el-menu-tooltip__trigger .el-icon) {
			margin-right: 0;
			font-size: 1.15rem;
		}
	}
}

/* 隐藏普通菜单项（el-menu-item）折叠后的纯文字 Tooltip 提示 */
:global(.el-popper.is-dark.el-tooltip__binder),
:global(.el-popper.is-dark:not(.el-menu--popup-container)) {
	display: none !important;
}

/* 隐藏有子菜单的项（el-sub-menu）折叠后弹出的子菜单大浮层 */
:global(.el-popper.el-menu--popup-container) {
	display: none !important;
}
</style>
