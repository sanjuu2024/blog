<template>
	<div class="app-loading">
		<slot
			name="loading"
			v-if="loading"
		>
			<div class="app-loading__inner">
				<div class="app-loading__visual">
					<img
						v-if="!imageFailed"
						:src="loadingImage"
						alt=""
						class="app-loading__image"
						@error="imageFailed = true"
					/>
					<i-lucide-loader
						v-else
						class="animate-spin text-gray-400"
					/>
				</div>
				<span
					v-if="!imageFailed"
					class="app-loading__text"
					>Loading......</span
				>
			</div>
		</slot>
		<slot v-else></slot>
	</div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import loadingImage from '@/assets/img/loading.webp';
defineOptions({
	name: 'AppLoading',
});

const props = defineProps<{
	loading: boolean;
}>();

// 直接显示图片，避免缓存命中时仍需等待 load 事件才移除 loader。
const imageFailed = ref(false);
watch(
	() => props.loading,
	(loading) => {
		if (loading) imageFailed.value = false;
	},
);
</script>

<style lang="scss" scoped>
.app-loading {
	width: 100%;
}

.app-loading__inner {
	grid-column: 1 / -1; // 当父容器是 grid 布局时，子元素占满整行；在非 Grid 容器中，grid-column 不会产生作用。
	min-height: 10rem;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
}

.app-loading__visual {
	display: flex;
	height: 6rem;
	align-items: center;
	justify-content: center;
	font-size: 1.5rem;
}

.app-loading__image {
	width: auto;
	height: 100%;
}

.app-loading__text {
	margin-top: 0.5rem;
	color: var(--app-text-muted);
	font-size: 0.875rem;
}
</style>
