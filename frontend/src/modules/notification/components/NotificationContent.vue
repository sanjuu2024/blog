<template>
	<div>
		<p
			ref="contentRef"
			class="notification-content"
			:class="{ 'notification-content--expanded': expanded }"
		>
			{{ content }}
		</p>
		<button
			v-if="overflows"
			type="button"
			class="notification-content__toggle"
			@click.stop="toggle"
		>
			{{ expanded ? '收起' : '展开' }}
		</button>
	</div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { useResizeObserver } from '@vueuse/core';

defineOptions({ name: 'NotificationContent' });

const props = defineProps<{ content: string }>();
const contentRef = ref<HTMLElement | null>(null);
const expanded = ref(false);
const overflows = ref(false);

function updateOverflow() {
	if (!contentRef.value || expanded.value) return;
	overflows.value = contentRef.value.scrollHeight - contentRef.value.clientHeight > 1;
}

useResizeObserver(contentRef, updateOverflow);
watch(
	() => props.content,
	() => nextTick(updateOverflow),
);

async function toggle() {
	expanded.value = !expanded.value;
	if (!expanded.value) {
		await nextTick();
		updateOverflow();
	}
}
</script>

<style scoped lang="scss">
.notification-content {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	line-clamp: 2;
	margin: 0;
	white-space: pre-wrap;
	overflow-wrap: anywhere;
	cursor: pointer;
}

.notification-content--expanded {
	display: block;
	-webkit-line-clamp: unset;
	line-clamp: unset;
}

.notification-content__toggle {
	display: block;
	margin: 0.25rem 0 0 auto;
	border: 0;
	background: transparent;
	color: var(--app-main);
	cursor: pointer;
}
</style>
