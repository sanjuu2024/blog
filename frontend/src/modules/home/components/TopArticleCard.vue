<template>
	<div class="card-wrapper">
		<div class="card-wrapper__inner">
			<div class="img">
				<RouterLink
					:to="{
						name: 'ArticleDetail',
						params: {
							articleId: article.id,
						},
					}"
				>
					<AppImage
						:url="article.coverUrl"
						alt="文章封面图片"
						:rounded="false"
						class="h-full rounded-none border border-(--app-border)"
					/>
				</RouterLink>
			</div>

			<div class="content">
				<p class="content__category">
					<component
						:is="getRouteIcon('category')"
						class="text-sm"
					/>

					<RouterLink
						:to="{
							name: 'Archive',
							query: {
								mode: 'category',
								categoryId: article.category.parent.id,
							},
						}"
					>
						{{ article.category.parent.name }}
					</RouterLink>
					/
					<RouterLink
						:to="{
							name: 'Archive',
							query: {
								mode: 'category',
								categoryId: article.category.id,
							},
						}"
					>
						{{ article.category.name }}
					</RouterLink>
				</p>

				<h3 class="content__title">
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
				</h3>

				<p class="content__summary line-clamp-3">
					{{ article.summary }}
				</p>

				<div class="content__meta">
					<component
						:is="getRouteIcon('calendar')"
						class="mr-2"
					/>
					<span>{{ formatDateTime(article.publishedAt) }}</span>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import type { PublicArticleListItem } from '@/modules/article/types/article';
import { formatDateTime } from '@/utils/datetime';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'TopArticleCard',
});

defineProps<{
	article: PublicArticleListItem;
}>();
</script>

<style lang="scss" scoped>
.card-wrapper {
	display: flex;
	flex-direction: column;
	border: 2px solid var(--app-border);
	transition:
		border 0.1s ease-in-out,
		transform 0.1s ease-in-out;

	&:hover,
	&:focus-within {
		border: 2px solid var(--app-main);
		transform: translateY(-0.2rem);

		.content__title {
			color: var(--app-main);
		}
	}

	.card-wrapper__inner {
		display: flex;
		flex: 1;
		flex-direction: column;
	}

	.img {
		height: 300px;
		flex-shrink: 0;
	}

	.content {
		display: flex;
		min-height: 200px;
		flex: 1;
		flex-direction: column;
		padding: 1rem;

		.content__category {
			display: flex;
			align-items: center;
			gap: 0.5rem;
			font-weight: 450;
			font-size: 0.9rem;
			margin-bottom: 0.5rem;
			transition: color 0.1s ease-in-out;

			> a:hover,
			> a:focus-visible {
				color: var(--app-main);
			}
		}

		.content__title {
			font-size: 1.5rem;
			font-weight: 600;
			margin-bottom: 0.5rem;
		}

		.content__summary {
			font-size: 0.9rem;
			margin-bottom: 0.5rem;
			color: var(--app-text-muted);
		}

		.content__meta {
			display: flex;
			align-items: center;
			margin-top: auto;
			font-size: 0.8rem;
			color: var(--app-text-muted);
		}
	}
}
</style>
