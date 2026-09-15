<template>
	<div>
		<p
			ref="contentRef"
			:id="`comment-content-${commentId}`"
			class="comment-content"
			:class="{ 'comment-content--expanded': contentExpanded }"
		>
			{{ content }}
		</p>

		<slot name="reason"></slot>

		<div
			v-if="showActions || contentOverflows"
			class="comment-content__bottom-toolbar"
		>
			<slot name="actions"></slot>
			<button
				v-if="contentOverflows"
				type="button"
				class="comment-content-toggle"
				:aria-controls="`comment-content-${commentId}`"
				:aria-expanded="contentExpanded"
				@click="toggleContent"
			>
				<span
					class="transition-transform duration-100 ease-in-out"
					:class="{ '-rotate-180': contentExpanded }"
				>
					<i-lucide-chevron-down />
				</span>
				{{ contentExpanded ? '收起' : '展开' }}
			</button>
		</div>
	</div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { useResizeObserver } from '@vueuse/core';

defineOptions({
	name: 'PublicCommentContent',
});

defineProps<{
	commentId: number;
	content: string;
	showActions?: boolean;
}>();

const contentRef = ref<HTMLElement | null>(null);
const contentExpanded = ref(false);
const contentOverflows = ref(false);

// 折叠状态下根据实际渲染高度判断是否需要展示“展开”按钮。
useResizeObserver(contentRef, () => {
	if (!contentRef.value || contentExpanded.value) return;
	contentOverflows.value = contentRef.value.scrollHeight > contentRef.value.clientHeight;
});

async function toggleContent() {
	contentExpanded.value = !contentExpanded.value;

	await nextTick();

	if (!contentExpanded.value && contentRef.value) {
		contentOverflows.value = contentRef.value.scrollHeight > contentRef.value.clientHeight;
	}

	contentRef.value?.closest<HTMLElement>('[data-comment-entry]')?.scrollIntoView({
		behavior: 'smooth',
		block: 'start',
	});
}
</script>

<style scoped lang="scss">
.comment-content {
	max-height: 10rem;
	overflow: hidden;
	margin-top: 0.4rem;
	overflow-wrap: anywhere;
	white-space: pre-wrap;
	line-height: 1.7;
}

.comment-content--expanded {
	max-height: none;
}

.comment-content__bottom-toolbar {
	display: flex;
	align-items: center;
	margin-top: 0.35rem;
	font-size: 0.9rem;
}

.comment-content-toggle {
	display: flex;
	align-items: center;
	gap: 0.3rem;
	margin-inline-start: auto;
	background: transparent;
	cursor: pointer;
	font-weight: bold;
}

.comment-content-toggle:hover,
.comment-content-toggle:focus-visible {
	color: var(--app-main);
}
</style>
