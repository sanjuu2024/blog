<template>
	<el-backtop
		target=".app-layout"
		class="app-backtop__button"
		:right="32"
		:bottom="32"
		:duration="100"
		:visibility-height="0"
	>
		<i-lucide-arrow-up class="text-xl" />
	</el-backtop>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppBacktop',
});
</script>

<style scoped lang="scss">
.app-backtop__button {
	width: 44px;
	height: 44px;
	border-radius: 9999px;
	background-color: var(--app-button-bg);
	color: white;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2px 4px rgb(0 0 0 / 20%);
	transition: background-color 0.1s ease;

	&:hover {
		background-color: var(--app-button-hover);
	}
}
</style>
