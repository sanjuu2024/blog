<template>
	<div class="app-loading">
		<slot
			name="loading"
			v-if="loading"
		>
			<div class="app-loading__inner">
				<div class="app-loading__spinner">
					<i-lucide-loader class="animate-spin text-2xl text-gray-400" />
				</div>
			</div>
		</slot>
		<slot v-else></slot>
	</div>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AppLoading',
});

defineProps<{
	loading: boolean;
}>();
</script>

<style lang="scss" scoped>
.app-loading {
	width: 100%;
}

.app-loading__inner {
	grid-column: 1 / -1; // 当父容器是 grid 布局时，子元素占满整行；在非 Grid 容器中，grid-column 不会产生作用。
	min-height: 10rem;
	display: flex;
	align-items: center;
	justify-content: center;
}
</style>
