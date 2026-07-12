<template>
	<div class="latest-article-list-item">
		<div class="time">
			{{ formatDateTime(article.publishedAt, 'YYYY-MM-DD') }}
		</div>

		<div class="content">
			<div class="title">
				<RouterLink
					:to="{
						name: 'ArticleDetail',
						params: {
							articleId: article.id,
						},
					}"
				>
					{{ article.title }}
				</RouterLink>
			</div>

			<div class="category">
				<component
					:is="getRouteIcon('category')"
					class="text-sm"
				/>

				<RouterLink
					:to="{
						name: 'CategoryArticlesPage',
						params: {
							categoryId: article.category.parent.id,
						},
					}"
				>
					{{ article.category.parent.name }}
				</RouterLink>
				/
				<RouterLink
					:to="{
						name: 'CategoryArticlesPage',
						params: {
							categoryId: article.category.id,
						},
					}"
				>
					{{ article.category.name }}
				</RouterLink>
			</div>

			<p class="summary line-clamp-2">
				{{ article.summary ? article.summary : '暂无摘要。' }}
			</p>
		</div>
	</div>
</template>

<script setup lang="ts">
import type { PublicArticleListItem } from '@/modules/article/types/article';
import { formatDateTime } from '@/utils/datetime';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'LatestArticleListItem',
});

defineProps<{
	article: PublicArticleListItem;
}>();
</script>

<style lang="scss" scoped>
.latest-article-list-item {
	display: flex;
	align-items: center;
	width: 100%;
	padding-block: 1rem;
	transition:
		color 0.1s ease-in-out,
		padding 0.1s ease-in-out;

	&:hover,
	&:focus-within {
		.title {
			color: var(--app-main);
		}

		padding-inline: 1rem;
	}

	.time {
		width: 180px;
		padding-inline: 1rem;
		text-align: center;
	}

	.content {
		flex: 1;

		.title {
			font-size: 1.5rem;
			font-weight: 600;
			margin-bottom: 0.5rem;
		}

		.category {
			display: flex;
			align-items: center;
			gap: 0.5rem;
			margin-bottom: 0.5rem;

			> a:hover,
			> a:focus-visible {
				color: var(--app-main);
			}
		}

		.summary {
			font-size: 0.9rem;
			color: var(--app-text-muted);
		}
	}
}

@media (max-width: 767px) {
	.latest-article-list-item {
		flex-direction: column;
		align-items: flex-start;
		gap: 0.5rem;

		.time {
			width: auto;
			padding-inline: 0;
			text-align: left;
		}
	}
}
</style>
