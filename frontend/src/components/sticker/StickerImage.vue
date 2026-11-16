<template>
	<span v-if="failed">[{{ name }}]</span>
	<span
		v-else
		class="sticker-image"
		:class="{ 'sticker-image--loading': !loaded }"
		:aria-busy="!loaded"
		:style="{
			'--sticker-min-height': minHeightRem === undefined ? undefined : `${minHeightRem}rem`,
		}"
	>
		<img
			:key="url"
			ref="image"
			class="sticker-image__image"
			:src="url"
			:alt="`[${name}]`"
			:title="name"
			loading="lazy"
			decoding="async"
			@load="handleLoad"
			@error="handleError"
		/>
		<el-skeleton
			v-if="!loaded"
			class="sticker-image__skeleton"
			:animated="true"
			aria-hidden="true"
		>
			<template #template>
				<el-skeleton-item
					variant="rect"
					class="sticker-image__placeholder"
				/>
			</template>
		</el-skeleton>
	</span>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{ name: string; url: string; minHeightRem?: number }>();
const emit = defineEmits<{ load: [event: Event] }>();
const image = ref<HTMLImageElement | null>(null);
const loaded = ref(false);
const failed = ref(false);
watch(
	() => props.url,
	() => {
		loaded.value = false;
		failed.value = false;
	},
);

function handleLoad(event: Event) {
	// URL 切换后的旧图片事件不能覆盖新状态，原事件继续交给选择器测量实际宽高比。
	if (event.target !== image.value) return;
	loaded.value = true;
	emit('load', event);
}

function handleError(event: Event) {
	if (event.target === image.value) failed.value = true;
}
</script>

<style scoped lang="scss">
.sticker-image {
	position: relative;
	display: inline-block;
	width: auto;
	max-width: none;
	height: var(--sticker-min-height, 1.5rem);
	vertical-align: text-bottom;
	margin-inline: 0.15rem;
}

.sticker-image__image {
	display: block;
	width: auto;
	max-width: none;
	height: 100%;
}

.sticker-image--loading {
	width: var(--sticker-min-height, 1.5rem);

	.sticker-image__image {
		// 保留真实图片与布局占位，让浏览器继续触发懒加载。
		position: absolute;
		width: 100%;
		opacity: 0;
	}
}

.sticker-image__skeleton {
	position: absolute;
	inset: 0;
	width: 100%;
	height: 100%;
	pointer-events: none;
}

.sticker-image__placeholder {
	display: block;
	width: 100%;
	height: 100%;
	border-radius: 0.25rem;
}
</style>
