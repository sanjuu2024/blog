<template>
	<div class="archive-page">
		<!-- 归档导航沿用原分类侧栏的布局与样式。 -->
		<div
			v-show="!isMobile || navigationDrawerVisible"
			class="category-list-wrapper shadow-sm"
		>
			<!-- 移动端布局下侧栏可关闭 -->
			<div class="category-list-wrapper__header">
				<button
					v-if="isMobile"
					class="category-list-wrapper__close-button"
					@click="navigationDrawerVisible = false"
				>
					<i-lucide-x />
				</button>
			</div>
			<div
				class="archive-modes"
				aria-label="归档浏览方式"
			>
				<el-radio-group
					:model-value="mode"
					aria-label="归档浏览方式"
					@change="changeMode"
				>
					<el-radio-button
						v-for="item in modes"
						:key="item.mode"
						:value="item.mode"
					>
						{{ item.label }}
					</el-radio-button>
				</el-radio-group>
			</div>
			<div
				class="archive-navigation app-scrollbar app-scrollbar--stable-both-edges"
				@click="closeNavigationOnSelect"
			>
				<div
					v-if="navigationLoading"
					class="category-list-wrapper__state"
				>
					<AppLoading :loading="true" />
				</div>
				<div
					v-else-if="navigationFailed"
					class="category-list-wrapper__state"
				>
					<p>获取归档导航失败</p>
					<el-button @click="retryNavigation">重试</el-button>
				</div>
				<template v-else-if="ready">
					<!-- 按时间 -->
					<template v-if="mode === 'time'">
						<div
							v-for="item in years"
							:key="item.year"
							class="year-article-nav-item"
						>
							<RouterLink
								class="year-article-nav-item__inner"
								:class="{ 'is-active': query?.year === item.year }"
								:to="{ name: 'Archive', query: { mode: 'time', year: item.year } }"
								@click="navigationDrawerVisible = false"
							>
								<h3 class="year-article-nav-item__name">{{ item.year }} 年</h3>
								<span class="year-article-nav-item__count"
									>{{ item.articleCount }} 篇</span
								>
							</RouterLink>
						</div>
						<p
							v-if="!years.length"
							class="category-list-wrapper__state"
						>
							暂无文章归档
						</p>
					</template>

					<!-- 按分类和按标签 -->
					<template v-else>
						<div class="all-article-item">
							<RouterLink
								class="all-article-item__inner"
								:class="{ 'is-active': !query?.categoryId && !query?.tagId }"
								:to="{ name: 'Archive', query: { mode } }"
								@click="navigationDrawerVisible = false"
							>
								<div class="all-article-item__title flex items-center gap-2">
									<i-lucide-layers class="text-sm" />
									<h3 class="all-article-item__name">全部文章</h3>
								</div>
							</RouterLink>
						</div>

						<!-- 按分类 -->
						<template v-if="mode === 'category'">
							<PrimaryCategoryList
								v-for="category in categories"
								:key="category.id"
								:category="category"
							/>
							<p
								v-if="!categories.length"
								class="category-list-wrapper__state"
							>
								暂无分类
							</p>
						</template>

						<!-- 按标签 -->
						<div
							v-else
							class="archive-tags"
						>
							<ArchiveTagCapsule
								v-for="tag in tags"
								:key="tag.id"
								:tag="tag"
								:active="query?.tagId === tag.id"
								@click="navigationDrawerVisible = false"
							/>
							<p
								v-if="!tags.length"
								class="category-list-wrapper__state"
							>
								暂无标签
							</p>
						</div>
					</template>
				</template>
			</div>
		</div>

		<!-- 右栏显示文章紧凑列表 -->
		<div
			ref="articleListRef"
			class="article-list-wrapper app-scrollbar app-scrollbar--stable"
		>
			<!-- 移动端的归档导航入口 -->
			<div
				v-show="isMobile && !navigationDrawerVisible"
				class="archive-page__mobile-header"
			>
				<button
					class="archive-page__mobile-header-button"
					@click="navigationDrawerVisible = true"
				>
					<i-lucide-layers />
					<!-- {{ title }} -->
					<span v-if="mode === 'time'">按时间</span>
					<span v-else-if="mode === 'category'">按分类</span>
					<span v-else-if="mode === 'tag'">按标签</span>
				</button>
			</div>

			<header class="archive-heading">
				<h1>{{ title }}</h1>
				<span v-if="loaded">{{ total }} 篇</span>
			</header>
			<ArchiveArticleItem
				v-for="article in articles"
				:key="article.id"
				:article="article"
			/>
			<div
				v-if="loadFailed"
				class="archive-state"
			>
				<p>获取文章失败</p>
				<el-button @click="retry">重试</el-button>
			</div>
			<p
				v-else-if="loaded && !articles.length"
				class="archive-state"
			>
				暂无文章
			</p>
			<AppLoadMoreTrigger
				v-else-if="query"
				:loading="loading"
				:has-next="hasNext"
				:disabled="loadFailed"
				:show-no-more="loaded && articles.length > 0"
				thing-str="文章"
				@load-more="loadMore"
			/>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import { useMediaQuery } from '@vueuse/core';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import PrimaryCategoryList from '@/modules/category/components/PrimaryCategoryList.vue';
import ArchiveArticleItem from '../components/ArchiveArticleItem.vue';
import ArchiveTagCapsule from '../components/ArchiveTagCapsule.vue';
import { useArchiveNavigation } from '../composables/useArchiveNavigation';
import { useArchiveArticles } from '../composables/useArchiveArticles';

defineOptions({
	name: 'ArchivePage',
});

const isMobile = useMediaQuery('(width < 1024px)');

const {
	timeZone,
	mode,
	changeMode,
	query,
	title,
	ready,
	years,
	categories,
	tags,
	navigationLoading,
	navigationFailed,
	retryNavigation,
} = useArchiveNavigation();
const { articles, total, loading, loaded, loadFailed, hasNext, retry, loadMore } =
	useArchiveArticles(query, timeZone);
const articleListRef = ref<HTMLElement | null>(null);
const modes = [
	{ mode: 'time', label: '按时间' },
	{ mode: 'category', label: '按分类' },
	{ mode: 'tag', label: '按标签' },
];

// 控制移动端布局下归档导航侧栏是否可见
const navigationDrawerVisible = ref(false);

function closeNavigationOnSelect(event: MouseEvent) {
	if ((event.target as Element).closest('a')) {
		navigationDrawerVisible.value = false;
	}
}

// 条件变化只重置文章列表，左侧导航保留滚动位置。
watch(
	() => JSON.stringify(query.value),
	async () => {
		await nextTick();
		if (articleListRef.value) {
			articleListRef.value.scrollTop = 0;
		}
	},
);
</script>

<style lang="scss" scoped>
.archive-page {
	position: relative;
	height: 100%;
	display: flex;
	min-height: 0;
	overflow: hidden;
}

.category-list-wrapper {
	display: flex;
	flex-direction: column;
	align-items: center;
	flex-shrink: 0;
	width: min(300px, 100vw);
	max-width: 100%;
	height: 100%;
	border: 1px solid var(--app-border);
	border-radius: 0.7rem;
	padding-inline: 0.5rem;
	overflow: hidden;
	padding-bottom: 0.5rem;

	.category-list-wrapper__header {
		display: flex;
		align-items: center;
		width: 100%;
		justify-content: flex-end;
		padding-block: 0.5rem;
	}

	.all-article-item {
		width: 100%;
		border-bottom: 1px solid var(--app-border);

		.all-article-item__inner {
			display: flex;
			align-items: center;
			justify-content: space-between;
			width: 100%;
			margin-bottom: 0.5rem;
			padding: 1.8rem 1rem;
			border-radius: 0.7rem;
			transition:
				background-color 0.2s ease,
				color 0.2s ease;

			&:hover,
			&.is-active {
				background-color: var(--app-archive-router-hover);
			}

			.all-article-item__name {
				font-weight: 700;
			}
		}
	}

	.year-article-nav-item {
		.year-article-nav-item__inner {
			display: flex;
			align-items: center;
			justify-content: space-between;
			width: 100%;
			margin-bottom: 0.25rem;
			padding: 1.25rem;
			border-radius: 0.7rem;
			transition:
				background-color 0.2s ease,
				color 0.2s ease;

			&:hover,
			&.is-active {
				background-color: var(--app-archive-router-hover);
			}

			.year-article-nav-item__name {
				font-weight: 700;
			}

			.year-article-nav-item__count {
				background: var(--app-surface-muted);
				color: var(--app-text-muted);
				font-size: 0.8rem;
				border-radius: 9999px;
				padding: 0 0.4rem;
			}
		}
	}

	.category-list-wrapper__state {
		width: 100%;
		padding: 1rem;
		color: var(--app-text-muted);
		font-size: 0.9rem;
		text-align: center;
	}
}

.archive-navigation {
	width: 100%;
	flex: 1;
	min-width: 0;
	min-height: 0;
	overflow-y: auto;
}

.archive-modes {
	width: 100%;
	padding-bottom: 1rem;
	padding-inline: 0.75rem;

	:deep(.el-radio-group) {
		width: 100%;
		flex-wrap: nowrap;
	}

	:deep(.el-radio-button) {
		flex: 1;
		min-width: 0;
	}

	:deep(.el-radio-button__inner) {
		width: 100%;
		padding-inline: 0.5rem;
	}
}

.archive-tags {
	display: flex;
	flex-wrap: wrap;
	min-width: 0;
	width: 100%;
	gap: 0.5rem;
	padding: 1rem 0.5rem;
}

.archive-heading {
	display: flex;
	align-items: last baseline;
	gap: 0.75rem;
	padding: 1rem 0;
	border-bottom: 1px solid var(--app-border);

	h1 {
		min-width: 0;
		font-size: 1.5rem;
		font-weight: bold;
		overflow-wrap: anywhere;
	}

	span {
		flex-shrink: 0;
		font-size: 0.85rem;
		color: var(--app-text-muted);
	}
}

.archive-state {
	padding: 2rem 0;
	text-align: center;
	color: var(--app-text-muted);
}

.article-list-wrapper {
	flex: 1;
	min-width: 0;
	margin-left: 2rem;
	height: 100%;
	overflow-y: auto;
	padding-inline: 0.75rem;
}

.archive-page__mobile-header {
	position: sticky;
	top: 0;
	height: var(--app-category-mobile-header-height);
	display: flex;
	align-items: center;
	justify-content: space-between;
	background-color: var(--app-bg);
	width: 100%;
	z-index: 99;
	font-size: 1.1rem;

	.archive-page__mobile-header-button {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		cursor: pointer;
		font-weight: 580;

		&:hover,
		&:focus-visible {
			color: var(--app-main);
		}
	}
}

@media (width < 1024px) {
	.category-list-wrapper {
		position: absolute;
		z-index: 100;
		inset-block: 0;
		left: 0;
		background-color: var(--app-bg);
	}

	.article-list-wrapper {
		margin-left: 0;
	}
}

@media (width < 500px) {
	.category-list-wrapper {
		width: 100%;
	}
}
</style>
