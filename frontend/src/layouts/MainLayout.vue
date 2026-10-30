<template>
	<div class="app-layout">
		<AppHeader class="app-layout__header" />
		<AppMain />
		<AppFooter />
	</div>
</template>

<script setup lang="ts">
import AppHeader from './components/AppHeader.vue';
import AppMain from './components/AppMain.vue';
import AppFooter from './components/AppFooter.vue';

defineOptions({
	name: 'MainLayout',
});
</script>

<style scoped lang="scss">
.app-layout {
	display: flex;
	flex-direction: column;
	min-height: 100dvh;

	// 由浏览器正常处理页面滚动，不需要让整个布局容器强制滚动。
	// overflow-y: scroll;
}
</style>
