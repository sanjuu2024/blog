<template>
	<div class="category-page">
		<!-- 分类列表侧栏 -->
		<div
			v-show="!isMobile || categoryDrawerVisible"
			ref="categoryListWrapperRef"
			class="category-list-wrapper app-scrollbar .app-scrollbar--stable-both-edges shadow-sm"
		>
			<!-- 移动端布局下侧栏可关闭 -->
			<div class="category-list-wrapper__header">
				<button
					v-if="isMobile"
					class="category-list-wrapper__close-button"
					@click="categoryDrawerVisible = false"
				>
					<i-lucide-x />
				</button>
			</div>
			<div class="all-article-item">
				<RouterLink
					class="all-article-item__inner"
					:to="{ name: 'CategoryAllArticles' }"
				>
					<div class="all-article-item__title flex items-center gap-2">
						<i-lucide-layers class="text-sm" />
						<h3 class="all-article-item__name">全部文章</h3>
					</div>
					<span class="all-article-item__count">{{ totalArticleCount }}</span>
				</RouterLink>
			</div>

			<PrimaryCategoryList
				v-for="category in enabledCategoryList"
				:key="category.id"
				:category="category"
			/>
			<div
				v-if="loading && !enabledCategoryList.length"
				class="category-list-wrapper__state"
			>
				<i-lucide-loader class="animate-spin text-xl" />
			</div>
			<p
				v-else-if="loadFailed && !enabledCategoryList.length"
				class="category-list-wrapper__state"
			>
				获取分类列表失败，请稍后重试
			</p>
			<p
				v-else-if="loaded && !enabledCategoryList.length"
				class="category-list-wrapper__state"
			>
				暂无分类
			</p>
		</div>

		<div class="article-list-wrapper app-scrollbar app-scrollbar--stable">
			<!-- 移动端的分类顶栏 -->
			<div
				v-show="isMobile && !categoryDrawerVisible"
				class="category-page__mobile-header"
			>
				<button
					class="category-page__mobile-header-button"
					@click="categoryDrawerVisible = true"
				>
					<i-lucide-layers />
					{{ currentCategoryName }}
				</button>
			</div>

			<RouterView #="{ Component }">
				<transition
					name="fade"
					mode="out-in"
				>
					<component :is="Component" />
				</transition>
			</RouterView>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import { computed, onMounted } from 'vue';
import PrimaryCategoryList from '../components/PrimaryCategoryList.vue';
import { useEnabledCategoryList } from '../composables/useEnabledCategoryList';
import { useRoute } from 'vue-router';
import { useMediaQuery } from '@vueuse/core';

const isMobile = useMediaQuery('(width < 1024px)');

const route = useRoute();

const categoryListWrapperRef = ref<HTMLElement | null>(null);

const { enabledCategoryList, loading, loadFailed, loaded, getEnabledCategoryList } =
	useEnabledCategoryList();

// 控制移动端布局下分类侧栏是否可见
const categoryDrawerVisible = ref(false);

// 移动端布局下找到当前显示的分类名称
const currentCategoryName = computed(() => {
	if (!activeCategoryId.value) return '全部文章';
	for (const c of enabledCategoryList.value) {
		if (c.id === activeCategoryId.value) return c.name;

		const child = c.children.find((child) => child.id === activeCategoryId.value);
		if (child) return child.name;
	}
	return '无效分类';
});

defineOptions({
	name: 'CategoryPage',
});

const totalArticleCount = computed(() => {
	return enabledCategoryList.value.reduce((total, category) => total + category.articleCount, 0);
});

onMounted(() => {
	getEnabledCategoryList();
});

// 当前查询的分类 id
const activeCategoryId = computed(() => {
	const categoryIdParam = route.params.categoryId;
	const categoryIdStr = Array.isArray(categoryIdParam) ? categoryIdParam[0] : categoryIdParam;
	const categoryId = Number(categoryIdStr);
	if (!categoryIdStr || !Number.isInteger(categoryId) || categoryId <= 0) {
		return undefined;
	}
	return categoryId;
});

// 将当前激活的分类路由（尽量）滚动到分类侧栏的中间位置
function scrollActiveCategoryToCenter() {
	if (!activeCategoryId.value || !categoryListWrapperRef.value) return;

	const container = categoryListWrapperRef.value;
	const target = container.querySelector<HTMLElement>(
		`[data-category-id="${activeCategoryId.value}"]`,
	);

	if (!target) return;

	const containerRect = container.getBoundingClientRect();
	const targetRect = target.getBoundingClientRect();

	const targetTop =
		container.scrollTop +
		targetRect.top -
		containerRect.top -
		container.clientHeight / 2 +
		targetRect.height / 2;

	container.scrollTo({
		top: targetTop,
		behavior: 'auto',
	});
}

// 监听路由和分类加载完成状态：
watch(
	() => [activeCategoryId.value, loaded.value, enabledCategoryList.value.length],
	async () => {
		categoryDrawerVisible.value = false;
		await nextTick();
		scrollActiveCategoryToCenter();
	},
);
</script>

<style lang="scss" scoped>
.category-page {
	height: 100%;
	display: flex;
}

.category-list-wrapper {
	display: flex;
	flex-direction: column;
	align-items: center;
	width: 300px;
	width: min(300px, 100vw);
	height: 100%;
	border: 1px solid var(--app-border);
	border-radius: 0.7rem;
	padding-inline: 0.5rem;
	overflow-y: auto;
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

			&:hover {
				background-color: var(--app-router-hover);
			}

			&.router-link-exact-active {
				background-color: var(--app-router-hover);
				color: var(--app-text);
			}

			.all-article-item__name {
				font-weight: 700;
			}

			.all-article-item__count {
				background: var(--app-surface-muted);
				color: var(--app-text-muted);
				font-weight: 550;
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

.article-list-wrapper {
	flex: 1;
	margin-left: 1rem;
	height: 100%;
	overflow-y: auto;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
	transform: translateY(0.25rem);
}

.fade-enter-active,
.fade-leave-active {
	transition:
		opacity 0.2s ease,
		transform 0.2s ease;
}

.fade-enter-to,
.fade-leave-from {
	opacity: 1;
	transform: translateY(0);
}

.category-page__mobile-header {
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

	.category-page__mobile-header-button {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		cursor: pointer;

		&:hover,
		&:focus-visible {
			color: var(--app-main);
		}
	}
}

@media (width < 1024px) {
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
