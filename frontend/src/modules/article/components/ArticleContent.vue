<template>
	<div
		class="article-content-wrapper"
		:class="{ 'article-content-wrapper-squeeze-to-the-right': expanded }"
		@click="closeCatalog"
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
						name: 'Archive',
						query: {
							mode: 'category',
							categoryId: article.category.parent.id,
						},
					}"
				>
					{{ article.category.parent.name }}
				</RouterLink>
				<span class="mx-2">></span>
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
			</div>

			<!-- 文章标签 -->
			<div
				class="article-tags mb-4 flex flex-wrap items-center justify-center gap-2"
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
				<!-- 浏览数 -->
				<div class="article-statis-item">
					<component
						:is="getRouteIcon('view')"
						class="mr-2"
					/>
					<span>{{ article.viewCount }}</span>
				</div>

				<!-- 评论数 -->
				<div class="article-statis-item">
					<component
						:is="getRouteIcon('comment')"
						class="mx-2"
					/>
					<span>{{ article.commentCount }}</span>
				</div>

				<!-- 点赞数 -->
				<button
					type="button"
					class="article-statis-item article-statis-like"
					:class="{ 'article-statis-like--liked': article.liked }"
					:aria-label="article.liked ? '取消文章点赞' : '点赞文章'"
					:aria-pressed="article.liked"
					:disabled="updatingLike"
					@click.stop="
						(e) => {
							if (!article?.liked) triggerConfetti(e);
							emit('toggle-like');
						}
					"
				>
					<component
						:is="getRouteIcon('like')"
						class="mx-2"
					/>
					<span>{{ article.likeCount }}</span>
				</button>
			</div>

			<!-- 文章摘要 -->
			<div
				class="article-summary"
				v-if="article.summary"
			>
				<p class="text-center text-sm text-(--app-text-muted)">{{ article.summary }}</p>
			</div>

			<!-- 文章内容 -->
			<!-- 决定还是不用 md-editor-v3 套，而是用 github-markdown-css + 自定义 css 类控制 -->
			<ArticleBody
				ref="bodyRef"
				:html="article.contentHtml"
				@rendered="refreshCatalog"
			/>

			<!-- 文章作者 & 发布时间 -->
			<div class="article-author-and-published-at">
				<PublicUserProfilePopover
					:user-id="article.author.id"
					:disabled="article.author.deleted"
				>
					<AppUserAvatar
						:avatar-url="article.author.avatarUrl"
						:name="article.author.nickname || article.author.username"
						:user-id="article.author.id"
						:deleted="article.author.deleted"
						:is-admin="article.author.role === 'ADMIN'"
						:size="32"
						class="mr-2"
					/>
					<span :class="{ 'article-author--deleted': article.author.deleted }">
						{{ article.author.nickname || article.author.username }}
					</span>
				</PublicUserProfilePopover>
				<span class="mx-4">·</span>
				<div class="flex items-center">
					<component
						:is="getRouteIcon('calendar')"
						class="mr-2"
					/>
					<span>{{ formatDateTime(article.publishedAt) }}</span>
				</div>
			</div>
		</div>

		<div
			v-else
			class="flex items-center justify-center"
		>
			<p>{{ errorMessage || '文章不存在或已被删除' }}</p>
		</div>
	</div>

	<ArticleLikeButton
		v-if="article"
		v-model:expanded="expanded"
		:offset-y="catalogList.length ? 190 : 122"
		:is-liked="article.liked"
		:like-count="article.likeCount"
		@toggle="emit('toggle-like')"
	/>

	<AppFixedButton
		v-if="article"
		class="article-comment-button"
		:class="{ 'article-comment-button-expanded': expanded }"
		placement="bottom-left"
		:offset-y="catalogList.length ? 100 : 32"
		:height-multiplier="1.5"
		aria-label="跳转到评论区"
		aria-controls="article-comments"
		@click="jumpToComments"
	>
		<div class="flex flex-col items-center">
			<component
				:is="getRouteIcon('comment')"
				class="text-xl"
			/>
			<span class="article-comment-button__count text-xs">{{ article.commentCount }}</span>
		</div>
	</AppFixedButton>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import type { PublicArticleDetailData } from '../types/article';
import AppImage from '@/components/AppImage.vue';
import AppFixedButton from '@/components/AppFixedButton.vue';
import AppUserAvatar from '@/components/AppUserAvatar.vue';
import PublicUserProfilePopover from '@/components/PublicUserProfilePopover.vue';
import getRouteIcon from '@/utils/getRouteIcon';
import ArticleCatalogSidebar from './ArticleCatalogSidebar.vue';
import { useArticleCatalog } from '../composables/useArticleCatalog';
import { useLikeConfetti } from '../composables/useLikeConfetti.ts';
import ArticleLikeButton from './ArticleLikeButton.vue';
import ArticleBody from './ArticleBody.vue';

// import { useTheme } from '@/composables/useTheme';
// const { resolvedTheme: ResolvedThemeName } = useTheme();

defineOptions({
	name: 'ArticleContent',
});

const { triggerConfetti } = useLikeConfetti();

defineProps<{
	article: PublicArticleDetailData | null;
	isLoading: boolean;
	errorMessage?: string;
	updatingLike?: boolean;
}>();

const emit = defineEmits<{
	'toggle-like': [];
	'jump-to-comments': [];
}>();

// 控制目录侧栏是否展开
const expanded = ref(false);

// 显示文章内容的容器
const bodyRef = ref<InstanceType<typeof ArticleBody> | null>(null);
const contentRef = computed(() => bodyRef.value?.element ?? null);
const { activeCatalogId, catalogList, refreshCatalog, scrollToHeading } =
	useArticleCatalog(contentRef);

// 点击文章详情区域时，关闭目录侧栏（侧栏展开状态下）
function closeCatalog() {
	if (expanded.value) {
		expanded.value = false;
	}
}

function jumpToComments() {
	closeCatalog();
	emit('jump-to-comments');
}
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

// .article-content-wrapper-squeeze-to-the-right {
// 	后续觉得不美观也不方便，遂不做目录展开时内容压缩了
// 	padding-left: var(--app-article-catalog-sidebar-width);
// }

.article-category {
	transition: color 0.1s ease-in-out;

	> a:hover,
	> a:focus-visible {
		color: var(--app-main);
	}
}

.article-tags {
	width: 100%;
}

.article-statis-item {
	display: flex;
	align-items: center;
	margin-right: 1rem;
}

.article-statis-like {
	padding: 0;
	border: 0;
	background: transparent;
	color: inherit;
	font: inherit;
	cursor: pointer;

	&:hover,
	&:focus-visible,
	&.article-statis-like--liked {
		color: var(--app-main);
	}

	&:focus-visible {
		outline: 2px solid var(--app-main);
		outline-offset: 2px;
	}
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

.article-author-and-published-at {
	margin-top: 3rem;
	margin-bottom: 1rem;
	display: flex;
	align-items: center;
	justify-content: center;
}

.article-author--deleted {
	color: var(--app-text-muted-more);
}

.article-comment-button-expanded {
	transform: translateX(calc(var(--app-article-catalog-sidebar-width) - 16px));
}

@media (width < 376px) {
	.article-title {
		h1 {
			font-size: 1.75rem;
		}
	}

	.article-author-and-published-at {
		flex-direction: column;
		gap: 0.5rem;
	}
}
</style>
