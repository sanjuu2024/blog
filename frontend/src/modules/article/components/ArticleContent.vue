<template>
	<div
		class="article-content-wrapper"
		:class="{ 'article-content-wrapper-squeeze-to-the-right': expanded }"
	>
		<div
			v-if="isLoading"
			class="flex items-center justify-center"
		>
			<i-lucide-loader class="animate-spin text-2xl text-gray-400" />
		</div>

		<div v-else-if="article">
			<ArticleCatalogSidebar
				v-model:expanded="expanded"
				:catalog-list="catalogList"
				:active-catalog-id="activeCatalogId"
				@select="scrollToHeading"
			/>

			<!-- 文章封面 -->
			<AppImage
				v-if="article.coverUrl"
				:url="article.coverUrl"
				object-fit="contain"
				alt="文章封面图"
				class="aspect-26/10 w-full shrink-0 rounded-xl"
			/>

			<!-- 文章标题 -->
			<div class="article-title mt-12 mb-4 flex items-center justify-center text-center">
				<h1 class="text-4xl font-bold">{{ article.title }}</h1>
			</div>

			<!-- 文章分类 -->
			<div class="article-category mb-4 flex items-center justify-center text-lg font-bold">
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
				<span class="mx-2">></span>
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

			<!-- 文章标签 -->
			<div
				class="article-tags mb-4 flex items-center justify-center gap-2"
				v-if="article.tags.length"
			>
				<component
					:is="getRouteIcon('tag')"
					class="text-sm"
				/>
				<AppTagCapsule
					v-for="tag in article.tags"
					:key="tag.id"
					:name="tag.name"
				/>
			</div>

			<!-- 文章统计信息 -->
			<div class="article-stats mb-4 flex items-center justify-center text-sm text-gray-500">
				<div class="article-statis-item">
					<component
						:is="getRouteIcon('view')"
						class="mr-2"
					/>
					<span>{{ article.viewCount }}</span>
				</div>
				<div class="article-statis-item">
					<component
						:is="getRouteIcon('comment')"
						class="mx-2"
					/>
					<span>{{ article.commentCount }}</span>
				</div>
				<div class="article-statis-item">
					<component
						:is="getRouteIcon('like')"
						class="mx-2"
					/>
					<span>{{ article.likeCount }}</span>
				</div>
			</div>

			<!-- 文章摘要 -->
			<div
				class="article-summary"
				v-if="article.summary"
			>
				<p class="text-sm text-(--app-text-muted)">{{ article.summary }}</p>
			</div>

			<!-- 文章内容 -->
			<!-- 决定还是不用 md-editor-v3 套，而是用 github-markdown-css + 自定义 css 类控制 -->
			<div
				ref="contentRef"
				v-html="article.contentHtml"
				class="article-markdown markdown-body"
			></div>

			<!-- 文章作者 & 发布时间 -->
			<div
				class="article-author-and-published-at mt-12 mb-4 flex items-center justify-center"
			>
				<i-solar-user-outline class="mr-2" />
				<span>{{ article.author.nickname }}</span>
				<span class="mx-4">·</span>
				<i-solar-calendar-outline class="mr-2" />
				<span>{{ formatDateTime(article.publishedAt) }}</span>
			</div>

			<!-- 文章发布时间 -->
			<div class="article-published-at flex items-center"></div>
		</div>

		<div
			v-else
			class="flex items-center justify-center"
		>
			<p>{{ errorMessage || '文章不存在或已被删除' }}</p>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import type { PublicArticleDetailData } from '../types/article';
import AppImage from '@/components/AppImage.vue';
import getRouteIcon from '@/utils/getRouteIcon';
import ArticleCatalogSidebar from './ArticleCatalogSidebar.vue';
import { useArticleCatalog } from '../composables/useArticleCatalog';
// 语法高亮渲染器
import { highlightCodeUnder } from '@/utils/prism';

// import { useTheme } from '@/composables/useTheme';
// const { resolvedTheme: ResolvedThemeName } = useTheme();

defineOptions({
	name: 'ArticleContent',
});

const props = defineProps<{
	article: PublicArticleDetailData | null;
	isLoading: boolean;
	errorMessage?: string;
}>();

// 控制目录侧栏是否展开
const expanded = ref(false);

// 显示文章内容的容器
const contentRef = ref<HTMLElement | null>(null);
const { activeCatalogId, catalogList, refreshCatalog, scrollToHeading } =
	useArticleCatalog(contentRef);

watch(
	() => props.article?.contentHtml,
	async () => {
		await nextTick();

		refreshCatalog();

		if (contentRef.value) {
			highlightCodeUnder(contentRef.value);
		}
	},
	{ immediate: true },
);
</script>

<style scoped lang="scss">
.article-content-wrapper {
	box-sizing: border-box;
	background-color: var(--app-bg);
	padding-block: 4rem;
	padding-inline: 0;
	transition:
		padding-left 0.2s ease,
		background-color 0.1s ease;
}

.article-content-wrapper-squeeze-to-the-right {
	padding-left: var(--app-article-catalog-sidebar-width);
}

.article-category {
	transition: color 0.1s ease-in-out;

	> a:hover,
	> a:focus-visible {
		color: var(--app-main);
	}
}

.article-statis-item {
	display: flex;
	align-items: center;
	margin-right: 1rem;
}

.article-summary {
	// text-indent: 2rem;
	// border-left: 4px solid var(--app-button-bg);
	display: flex;
	align-items: center;
	justify-content: center;
	text-indent: 2rem;
	margin-block: 1rem;
	margin-inline: auto;
	max-width: var(--app-article-detail-width);

	p::before {
		content: '“';
		font-size: 1.5rem;
	}

	p::after {
		content: '”';
		font-size: 1.5rem;
	}
}
</style>
