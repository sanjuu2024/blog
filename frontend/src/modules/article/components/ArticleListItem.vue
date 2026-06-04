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
					<div
						class="relative aspect-16/10 w-44 shrink-0 overflow-hidden rounded border border-gray-200"
					>
						<!-- 加载中 -->
						<div
							v-if="article.coverUrl && !failed && !loaded"
							class="absolute inset-0 flex h-full w-full items-center justify-center bg-gray-100 text-gray-300"
						>
							<!-- tailwind css 自带的 animate-spin -->
							<i-lucide-loader class="animate-spin text-xl" />
						</div>

						<!-- 加载成功 -->
						<img
							v-if="article.coverUrl && !failed"
							:src="article.coverUrl"
							alt="文章封面"
							class="absolute inset-0 h-full w-full rounded object-cover"
							:class="{ 'opacity-0': !loaded }"
							loading="lazy"
							decoding="async"
							@load="handleCoverLoad"
							@error="handleCoverError"
						/>

						<!-- 无封面 -->
						<div
							v-else-if="!article.coverUrl"
							class="absolute inset-0 flex h-full w-full items-center justify-center rounded bg-gray-100 text-gray-400"
						>
							<i-lucide-image class="text-xl" />
						</div>

						<!-- 加载失败 -->
						<div
							v-else
							class="absolute inset-0 flex h-full w-full items-center justify-center rounded bg-gray-100 text-gray-400"
						>
							<i-lucide-image-off class="text-xl" />
						</div>
					</div>
				</RouterLink>
			</div>

			<div class="right ml-8 flex flex-col justify-between">
				<div class="right__inner-main mb-4 py-2">
					<!-- 文章标题 & 分类 & 摘要 & 标签 -->
					<!-- 标题 -->
					<RouterLink :to="`/articles/${article.id}`">
						<h3
							class="article-list-item-title mb-4 text-xl font-bold transition-colors"
							:class="{ 'text-(--app-button-bg)': coverHovered }"
						>
							{{ article.title }}
						</h3>
					</RouterLink>

					<!-- 分类 -->
					<div class="article-list-item-category mb-2 flex items-center text-sm">
						<component
							:is="getRouteIcon('category')"
							class="mr-2"
						/>
						<span type="warning">{{
							article.category.parent.name + ' > ' + article.category.name
						}}</span>
					</div>

					<!-- 标签 -->
					<div
						class="article-list-item-tags mb-2 flex items-center text-sm"
						v-if="article.tags.length > 0"
					>
						<component
							:is="getRouteIcon('tag')"
							class="mr-2"
						/>
						<span
							v-for="tag in article.tags"
							:key="tag.id"
							class="mr-2 rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500"
						>
							#{{ tag.name }}
						</span>
					</div>

					<!-- 摘要 -->
					<div>
						<p class="line-clamp-2 text-sm">{{ article.summary }}</p>
					</div>
				</div>

				<div class="right__inner-footer flex w-full text-sm text-gray-400">
					<!-- 发布时间 -->
					<div class="article-list-item-published-at flex items-center">
						<i-lucide-calendar class="mr-2" />
						<p>{{ formatDateTime(article.publishedAt) }}</p>
					</div>

					<span class="mx-3">·</span>

					<!-- 阅读数 -->
					<div class="article-list-item-view-count flex items-center">
						<i-lucide-eye class="mr-2" />
						<span>{{ article.viewCount }}</span>
					</div>
				</div>
			</div>
		</div>

		<div class="article-list-item-pin ml-4">
			<i-solar-pin-bold
				v-if="article.isTop"
				style="color: var(--app-button-bg)"
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

defineOptions({
	name: 'ArticleListItem',
});

defineProps<{
	article: PublicArticleListItem;
}>();

// 控制图片是否显示 加载失败 / 加载中 的图标
const failed = ref<boolean>(false);
const loaded = ref<boolean>(false);

function handleCoverLoad() {
	failed.value = false;
	loaded.value = true;
}

function handleCoverError() {
	failed.value = true;
	loaded.value = false;
}

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
</style>
