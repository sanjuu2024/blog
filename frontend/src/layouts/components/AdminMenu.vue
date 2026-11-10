<template>
	<!-- 遍历一级路由 -->
	<template
		v-for="route in visibleMenuList"
		:key="route.path"
	>
		<!-- 🍰如果没有子路由那就是普通菜单 -->
		<template v-if="!getVisibleChildren(route).length">
			<el-menu-item
				:index="route.path"
				:key="route.path"
				@click="() => router.push(route.path)"
			>
				<!-- 🍉动态渲染不同路由对应的不同的icon组件 -->
				<el-icon
					v-if="getRouteIcon(route.meta?.icon)"
					size="1.3rem"
				>
					<component :is="getRouteIcon(route.meta?.icon)"></component>
				</el-icon>
				<template #title>
					<span>{{ route.meta?.title }}</span>
				</template>
			</el-menu-item>
		</template>

		<!-- 🍰如果有子路由那就是折叠菜单 -->
		<template v-else>
			<el-sub-menu :index="route.path">
				<!-- 然后这个template就是单纯语法要求了，具名插槽显示菜单title -->
				<template #title>
					<el-icon
						v-if="getRouteIcon(route.meta?.icon)"
						size="1.3rem"
					>
						<component :is="getRouteIcon(route.meta?.icon)"></component>
					</el-icon>
					<span>{{ route.meta?.title }}</span>
				</template>
				<AdminMenu :menuList="getVisibleChildren(route)"></AdminMenu>
			</el-sub-menu>
		</template>
	</template>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'AdminMenu',
});

const router = useRouter();

const props = withDefaults(
	defineProps<{
		menuList?: RouteRecordRaw[];
	}>(),
	{
		menuList: () => [], // 🔺数组是引用类型！不能直接赋值[]，要写成 getter
	},
);

const visibleMenuList = computed(() => props.menuList.filter((route) => !route.meta?.hidden));

function getVisibleChildren(route: RouteRecordRaw) {
	return route.children?.filter((child) => !child.meta?.hidden) ?? [];
}
</script>
