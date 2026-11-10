<template>
	<div class="app-image">
		<!-- 加载中 -->
		<div
			v-if="url && !failed && !loaded"
			class="absolute inset-0 flex h-full w-full items-center justify-center bg-(--app-default-img-bg-color) text-(--app-default-img-text-color)"
		>
			<!-- tailwind css 自带的 animate-spin -->
			<i-lucide-loader class="animate-spin text-xl" />
		</div>

		<!-- 加载成功 -->
		<img
			v-if="url && !failed"
			:src="url"
			:alt="alt || '图片'"
			class="absolute inset-0 h-full w-full"
			:class="{
				rounded: rounded,
				'opacity-0': !loaded,
				'object-cover': objectFit === 'cover',
				'object-contain': objectFit === 'contain',
				'object-fill': objectFit === 'fill',
				'object-none': objectFit === 'none',
				'object-scale-down': objectFit === 'scale-down',
			}"
			loading="lazy"
			decoding="async"
			@load="handleCoverLoad"
			@error="handleCoverError"
		/>

		<!-- 无封面 -->
		<div
			v-else-if="!url"
			class="absolute inset-0 flex h-full w-full items-center justify-center bg-(--app-default-img-bg-color) text-(--app-default-img-text-color)"
			:class="{
				rounded: rounded,
			}"
		>
			<i-lucide-image class="text-xl" />
		</div>

		<!-- 加载失败 -->
		<div
			v-else
			class="absolute inset-0 flex h-full w-full items-center justify-center bg-(--app-default-img-bg-color) text-(--app-default-img-text-color)"
			:class="{
				rounded: rounded,
			}"
		>
			<i-lucide-image-off class="text-xl" />
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
defineOptions({
	name: 'AppImage',
});

const props = withDefaults(
	defineProps<{
		url?: string;
		alt?: string;
		objectFit?: 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';
		rounded?: boolean;
	}>(),
	{
		objectFit: 'cover',
		rounded: true,
	},
);

// 控制图片是否显示 加载失败 / 加载中 的图标
const failed = ref<boolean>(false);
const loaded = ref<boolean>(false);

function handleCoverLoad() {
	failed.value = false;
	loaded.value = true;
}

function handleCoverError() {
	failed.value = true;
	loaded.value = false;
}

watch(
	() => props.url,
	() => {
		failed.value = false;
		loaded.value = false;
	},
);
</script>

<style scoped lang="scss">
.app-image {
	position: relative;
	overflow: hidden;
}
</style>
