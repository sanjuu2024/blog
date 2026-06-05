<template>
	<main
		class="admin-main"
		ref="adminMainRef"
	>
		<div class="admin-main__inner">
			<RouterView v-slot="{ Component }">
				<transition
					name="fade"
					mode="out-in"
				>
					<component :is="Component" />
				</transition>
			</RouterView>
		</div>
	</main>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue';
import { useRoute } from 'vue-router';

defineOptions({
	name: 'AdminMain',
});

const route = useRoute();
const adminMainRef = ref<HTMLElement>();

watch(
	() => route.fullPath,
	async () => {
		// 路由切换后重置滚动条位置
		await nextTick();
		adminMainRef.value?.scrollTo({
			top: 0,
			left: 0,
		});
	},
);
</script>

<style lang="scss" scoped>
/* 设计上后台管理模块只有 main 区域内部会有滚动条，整个页面的高度是直接固定的。（前台的话就是页面级滚动条） */
/* 也因此 src/router/index.ts 中的路由切换后滚动条位置重置只对前台有效，后台这边需要自己写滚动条位置重置逻辑。 */
.admin-main {
	position: absolute;
	top: var(--app-admin-header-height);
	left: var(--app-admin-sidebar-width);
	width: calc(100% - var(--app-admin-sidebar-width));
	height: calc(100dvh - var(--app-admin-header-height));
	overflow: auto;
	transition:
		left 0.2s ease,
		width 0.2s ease;
}

.admin-main__inner {
	padding-inline: var(--app-admin-main-padding-x);
	padding-block: var(--app-admin-main-padding-y);
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
	transform: translateY(0.25rem);
}

.fade-enter-active,
.fade-leave-active {
	transition:
		opacity 0.2s ease,
		transform 0.2s ease;
}

.fade-enter-to,
.fade-leave-from {
	opacity: 1;
	transform: translateY(0);
}
</style>
