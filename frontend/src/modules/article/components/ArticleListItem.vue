<template>
	<div class="article-list-item flex items-center justify-between px-4">
		<div class="article-list-item__inner group flex w-full items-center">
			<div class="left flex items-center">
				<!-- 文章封面图 -->
				<RouterLink
					:to="`/articles/${article.id}`"
					@mouseenter="coverHovered = true"
					@mouseleave="coverHovered = false"
				>
					<AppImage
						:url="article.coverUrl"
						alt="文章封面图"
						class="aspect-16/13 w-44 shrink-0 rounded border border-(--app-border)"
					/>
				</RouterLink>
			</div>

			<div class="right ml-8 flex flex-col justify-between">
				<div class="right__inner-main mb-4 py-2">
					<!-- 文章标题 & 分类 & 摘要 & 标签 -->
					<!-- 标题 -->
					<RouterLink :to="`/articles/${article.id}`">
						<!-- 搜索高亮 HTML 已由后端转义，只保留受控的 mark 标签 -->
						<h3
							v-if="article.highlightedTitle"
							class="article-list-item-title mb-4 text-2xl font-bold transition-colors"
							:class="{ 'text-(--app-button-bg)': coverHovered }"
							v-html="article.highlightedTitle"
						></h3>
						<h3
							v-else
							class="article-list-item-title mb-4 text-2xl font-bold transition-colors"
							:class="{ 'text-(--app-button-bg)': coverHovered }"
						>
							{{ article.title }}
						</h3>
					</RouterLink>

					<!-- 分类 -->
					<div class="article-list-item-category mb-2 flex items-center text-sm">
						<component
							:is="getRouteIcon('category')"
							class="mr-2 text-sm"
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
						<span class="mx-2">/</span>
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

					<!-- 标签 -->
					<div
						class="article-list-item-tags mb-2 flex items-center gap-2 text-sm"
						v-if="article.tags.length > 0"
					>
						<component :is="getRouteIcon('tag')" />
						<AppTagCapsule
							v-for="tag in article.tags"
							:key="tag.id"
							:name="tag.name"
						/>
					</div>
					<div
						v-else
						class="article-list-item-tags mb-2 flex items-center gap-2 text-sm"
					>
						<component :is="getRouteIcon('tag')" />
						<span>暂无标签</span>
					</div>

					<!-- 摘要 -->
					<div>
						<!-- 摘要或正文片段与标题使用相同的后端安全高亮格式 -->
						<p
							v-if="article.searchSnippet"
							class="line-clamp-2 text-sm"
							v-html="article.searchSnippet"
						></p>
						<p
							v-else
							class="line-clamp-2 text-sm"
						>
							{{ article.summary || '暂无摘要' }}
						</p>
					</div>
				</div>

				<div class="right__inner-footer flex w-full text-sm text-gray-400">
					<!-- 发布时间 -->
					<div class="article-list-item-published-at flex items-center">
						<i-solar-calendar-outline class="mr-2" />
						<p>{{ formatDateTime(article.publishedAt) }}</p>
					</div>

					<span class="mx-3">·</span>

					<!-- 阅读数 -->
					<div class="article-list-item-view-count flex items-center">
						<i-solar-eye-outline class="mr-2" />
						<span>{{ article.viewCount }}</span>
					</div>
				</div>
			</div>
		</div>

		<div class="article-list-item-pin ml-4">
			<i-solar-pin-bold
				v-if="article.isTop"
				style="color: var(--app-main)"
			/>
			<!-- <i-solar-pin-outline v-else /> -->
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import { type PublicArticleListItem } from '../types/article';
import getRouteIcon from '@/utils/getRouteIcon';
import AppImage from '@/components/AppImage.vue';
import AppTagCapsule from '@/components/AppTagCapsule.vue';

defineOptions({
	name: 'ArticleListItem',
});

defineProps<{
	article: PublicArticleListItem;
}>();

// 控制鼠标悬浮在封面图上时高亮标题
const coverHovered = ref(false);
</script>

<style scoped lang="scss">
.article-list-item__inner {
	padding: 16px;
}

.right {
	flex: 1;
}

.article-list-item-title:hover {
	color: var(--app-button-bg);
}

.article-list-item-category {
	transition: color 0.1s ease-in-out;

	> a:hover,
	> a:focus-visible {
		color: var(--app-main);
	}
}

:deep(.article-search-highlight) {
	padding-inline: 0.2em;
	border-radius: 2px;
	background-color: yellow;
	color: inherit;
	font-weight: 700;
}
</style>
