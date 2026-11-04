<template>
	<RouterLink
		class="archive-tag-capsule"
		:class="{ 'is-active': active }"
		:to="{ name: 'Archive', query: { mode: 'tag', tagId: tag.id } }"
		:aria-current="active ? 'page' : undefined"
	>
		<div class="archive-tag-capsule__inner">#{{ tag.name }} ({{ tag.articleCount }})</div>
	</RouterLink>
</template>

<script setup lang="ts">
import type { PublicTagItem } from '@/modules/tag/types/tag';

defineOptions({
	name: 'ArchiveTagCapsule',
});

withDefaults(
	defineProps<{
		tag: PublicTagItem;
		active?: boolean;
	}>(),
	{ active: false },
);
</script>

<style lang="scss" scoped>
.archive-tag-capsule {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	min-width: 0;
	max-width: 100%;
	padding: 0.25rem 0.5rem;
	border-radius: 8px;
	background-color: var(--app-archive-tag-capsule-bg-color);
	color: var(--app-tag-capsule-text-color);
	font-size: 0.875rem;
	transition:
		background-color 0.2s ease,
		color 0.2s ease;

	&:hover,
	&:focus-visible,
	&.is-active {
		background-color: var(--app-archive-router-hover);
		color: var(--app-text);
	}
}

.archive-tag-capsule__inner {
	// 允许长中文标签和连续英文串在胶囊内换行，避免撑宽侧栏。
	min-width: 0;
	overflow-wrap: anywhere;
}
</style>
