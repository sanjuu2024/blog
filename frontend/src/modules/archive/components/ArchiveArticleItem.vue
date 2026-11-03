<template>
	<article class="archive-article">
		<time
			:datetime="article.publishedAt"
			class="archive-article__date"
		>
			{{ publishedDate }}
		</time>

		<RouterLink
			class="archive-article__title"
			:to="`/articles/${article.id}`"
		>
			{{ article.title }}
		</RouterLink>

		<div class="archive-article__category">
			<component :is="getRouteIcon('category')"></component>
			<RouterLink
				:to="{
					name: 'Archive',
					query: { mode: 'category', categoryId: article.category.parent.id },
				}"
			>
				{{ article.category.parent.name }}
			</RouterLink>
			<span> / </span>
			<RouterLink
				:to="{
					name: 'Archive',
					query: { mode: 'category', categoryId: article.category.id },
				}"
			>
				{{ article.category.name }}
			</RouterLink>
		</div>
	</article>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import type { ArchiveArticle } from '../types/archive';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'ArchiveArticleItem',
});

const props = defineProps<{
	article: ArchiveArticle;
}>();

// 与文章、评论等页面保持一致，按浏览器当地时区展示日期。
const publishedDate = computed(() => formatDateTime(props.article.publishedAt, 'YYYY-MM-DD'));
</script>

<style lang="scss" scoped>
.archive-article {
	display: grid;
	grid-template-columns: 5rem minmax(0, 1fr) auto;
	align-items: center;
	gap: 1.25rem;
	padding: 1rem;
	border-bottom: 1px solid var(--app-border);
}

.archive-article__date,
.archive-article__category {
	display: flex;
	align-items: center;
	gap: 0.25rem;
	font-size: 0.9rem;
	color: var(--app-text-muted-more);

	a:hover,
	a:focus-visible {
		font-weight: bold;
		color: var(--app-main);
	}
}

.archive-article__title {
	font-weight: 580;
	overflow-wrap: anywhere;
}

.archive-article a:hover,
.archive-article a:focus-visible {
	color: var(--app-main);
}

@media (width < 768px) {
	.archive-article {
		grid-template-columns: auto minmax(0, 1fr);
		gap: 0.5rem 1rem;
	}

	.archive-article__title {
		grid-column: 1 / -1;
		grid-row: 1;
	}
}
</style>
