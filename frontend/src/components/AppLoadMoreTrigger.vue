<template>
	<!-- 滚动加载更多的触发器 -->
	<div
		ref="triggerRef"
		class="flex h-12 items-center justify-center"
	>
		<i-lucide-loader
			v-if="props.loading"
			class="animate-spin text-xl text-gray-400"
		/>
		<span
			v-else-if="!props.hasNext && !props.disabled"
			class="text-sm text-gray-400"
		>
			没有更多{{ props.thingStr }}了
		</span>
	</div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useIntersectionObserver } from '@vueuse/core';

const triggerRef = ref<HTMLElement | null>(null);

defineOptions({
	name: 'AppLoadMoreTrigger',
});

const props = withDefaults(
	defineProps<{
		loading?: boolean;
		hasNext?: boolean;
		disabled?: boolean; // e.g. 网络原因，以防无限重新请求，可以暂时禁用，改为手动“加载失败，点击重试”
		rootMargin?: string;
		thingStr?: string;
	}>(),
	{
		loading: false,
		hasNext: false,
		disabled: false,
		rootMargin: '200px',
		thingStr: '内容',
	},
);

const emit = defineEmits<{
	loadMore: [];
}>();

useIntersectionObserver(
	triggerRef,
	([{ isIntersecting }]) => {
		if (isIntersecting && props.hasNext && !props.loading && !props.disabled) {
			emit('loadMore');
		}
	},
	{
		rootMargin: props.rootMargin, // 距离底部还有指定距离时就提前加载下一页
	},
);
</script>

<style></style>
