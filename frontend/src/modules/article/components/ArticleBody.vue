<template>
	<div
		ref="element"
		v-html="html"
		class="article-markdown markdown-body"
	></div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
// 语法高亮渲染器
import { highlightCodeUnder } from '@/utils/prism';
import { renderImageCaptions } from '../utils/renderImageCaptions';

const props = defineProps<{ html: string }>();
const emit = defineEmits<{ rendered: [] }>();
const element = ref<HTMLElement | null>(null);
defineExpose({ element });

watch(
	() => props.html,
	async (_, __, onCleanup) => {
		let cancelled = false;
		onCleanup(() => {
			cancelled = true;
		});
		await nextTick();
		if (!element.value || cancelled) return;
		const contentElement = element.value;
		emit('rendered');
		// 将图片 title 转成图注并移除原生悬浮提示，保留 alt、尺寸和链接。
		renderImageCaptions(contentElement);
		const contentText = contentElement.textContent || '';
		if (
			contentText.includes('$') ||
			contentText.includes('\\(') ||
			contentText.includes('\\[')
		) {
			const { default: renderMathInElement } = await import('katex/contrib/auto-render');
			// HTML 变化或组件卸载后，不再向新内容写入旧请求的渲染结果。
			if (cancelled || element.value !== contentElement) return;
			renderMathInElement(contentElement, {
				delimiters: [
					{ left: '$$', right: '$$', display: true },
					{ left: '\\[', right: '\\]', display: true },
					{ left: '$', right: '$', display: false },
					{ left: '\\(', right: '\\)', display: false },
				],
				throwOnError: false,
				trust: false,
				ignoredClasses: ['article-image__caption'],
			});
		}
		highlightCodeUnder(contentElement);
	},
	{ immediate: true },
);
</script>

<style scoped lang="scss">
.markdown-body {
	// 默认会有 45 px 的内边距，移动端布局下内容区域过窄
	padding-inline: 0.5rem;
}
</style>
