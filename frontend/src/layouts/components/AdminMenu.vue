<template>
	<!-- 遍历一级路由 -->
	<template
		v-for="route in menuList"
		:key="route.path"
	>
		<!-- 🍰如果没有子路由那就是普通菜单 -->
		<template v-if="!route.children">
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
				<AdminMenu :menuList="route.children"></AdminMenu>
			</el-sub-menu>
		</template>
	</template>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'AdminMenu',
});

const router = useRouter();

withDefaults(
	defineProps<{
		menuList?: RouteRecordRaw[];
	}>(),
	{
		menuList: () => [], // 🔺数组是引用类型！不能直接赋值[]，要写成 getter
	},
);
</script>

<style></style>
