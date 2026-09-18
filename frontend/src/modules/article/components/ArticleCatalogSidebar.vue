<template>
	<Teleport
		v-if="catalogList.length"
		to="body"
	>
		<button
			class="article-catalog-sidebar-button"
			:class="{ 'article-catalog-sidebar-button-expanded': expanded }"
			aria-label="打开/关闭文章目录侧栏"
			:aria-expanded="expanded"
			@click="expanded = !expanded"
		>
			<i-lucide-list class="text-xl" />
		</button>

		<aside
			ref="sidebarRef"
			class="article-catalog-sidebar"
			:class="{ 'article-catalog-sidebar-expanded': expanded }"
		>
			<p class="article-catalog-sidebar__title">目录</p>
			<button
				v-for="catalogItem in catalogList"
				:key="catalogItem.id"
				type="button"
				class="article-catalog-sidebar__item"
				:class="{ 'is-active': catalogItem.id === displayedActiveCatalogId }"
				:data-catalog-id="catalogItem.id"
				:style="{ paddingLeft: `${catalogItem.level - 0.5}rem` }"
				@click="selectCatalogItem(catalogItem.id)"
			>
				{{ catalogItem.text }}
			</button>
		</aside>
	</Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { useEventListener } from '@vueuse/core';
import type { ArticleCatalogItem } from '../composables/useArticleCatalog';

defineOptions({
	name: 'ArticleCatalogSidebar',
});

const expanded = defineModel<boolean>('expanded', {
	required: true,
});

const props = defineProps<{
	catalogList: ArticleCatalogItem[];
	activeCatalogId?: string;
}>();

const emit = defineEmits<{
	select: [id: string];
}>();

const sidebarRef = ref<HTMLElement | null>(null);
const lockedCatalogId = ref('');

const displayedActiveCatalogId = computed(() => {
	return lockedCatalogId.value || props.activeCatalogId;
});

function selectCatalogItem(id: string) {
	// 点击目录时，先让目录侧栏自己立刻定位到目标项。
	// 后续文章正文会平滑滚动，但侧栏不再跟着中间经过的标题慢慢滑动。
	lockedCatalogId.value = id;
	scrollActiveCatalogItemIntoMiddle(id, 'auto');
	emit('select', id);
}

// 当前高亮目录项变化时，让侧栏内部也自动滚动。
// 目标不是把目录项滚到最顶端，而是尽量放到侧栏中间，
// 这样文章滚到很后面时，用户不用手动滚目录才能看到当前项。
function scrollActiveCatalogItemIntoMiddle(
	activeCatalogId: string,
	behavior: 'auto' | 'smooth' = 'smooth',
) {
	const sidebarElement = sidebarRef.value;

	if (!sidebarElement) return;

	const catalogItemElement = Array.from(
		sidebarElement.querySelectorAll<HTMLElement>('.article-catalog-sidebar__item'),
	).find((element) => element.dataset.catalogId === activeCatalogId);

	if (!catalogItemElement) return;

	const nextScrollTop =
		catalogItemElement.offsetTop -
		sidebarElement.clientHeight / 2 +
		catalogItemElement.offsetHeight / 2;

	sidebarElement.scrollTo({
		top: Math.max(0, nextScrollTop),
		behavior,
	});
}

watch(
	() => [props.activeCatalogId, expanded.value] as const,
	async ([activeCatalogId, isExpanded]) => {
		if (!activeCatalogId || !isExpanded) return;

		// 点击目录后的文章平滑滚动过程中，activeCatalogId 会经过多个中间标题。
		// 此时先锁定用户点击的目标项，避免侧栏跟着中间标题慢慢滚动。
		if (lockedCatalogId.value && activeCatalogId !== lockedCatalogId.value) {
			return;
		}

		if (activeCatalogId === lockedCatalogId.value) {
			lockedCatalogId.value = '';
		}

		await nextTick();
		scrollActiveCatalogItemIntoMiddle(activeCatalogId);
	},
);

// 监听 esc 键，关闭目录侧栏
useEventListener(document, 'keydown', (event: KeyboardEvent) => {
	if (event.key !== 'Escape' || !expanded.value) {
		return;
	}

	// 关闭侧边栏
	expanded.value = false;
});
</script>

<style lang="scss" scoped>
// 侧栏拉开/关上的按钮
.article-catalog-sidebar-button {
	position: fixed;
	left: 32px;
	bottom: 32px;
	width: 44px;
	height: 44px;
	border-radius: 9999px;
	background-color: var(--app-button-bg);
	color: white;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2px 4px rgb(0 0 0 / 20%);
	transition:
		transform 0.2s ease,
		background-color 0.1s ease;

	&:hover {
		background-color: var(--app-button-hover);
	}

	&:focus-visible {
		outline: 2px solid var(--app-main);
		outline-offset: 2px;
	}

	z-index: 999;
	cursor: pointer;
}

// 侧栏
.article-catalog-sidebar {
	position: fixed;
	top: var(--app-header-height);
	left: calc(-1 * var(--app-article-catalog-sidebar-width));
	width: var(--app-article-catalog-sidebar-width);
	height: calc(100dvh - var(--app-header-height));
	overflow: auto;
	padding: 1rem;
	background-color: var(--app-surface);
	box-shadow: 2px 2px 2px rgb(0 0 0 / 10%);
	transition:
		transform 0.2s ease,
		background-color 0.1s ease;
	z-index: 999;

	&::-webkit-scrollbar {
		width: 10px;
	}

	&::-webkit-scrollbar-thumb {
		border-radius: 999px;
	}

	&::-webkit-scrollbar-thumb:hover {
		background: rgb(228 228 228);
	}

	&::-webkit-scrollbar-track {
		background: transparent;
	}
}

.article-catalog-sidebar-expanded {
	transform: translateX(var(--app-article-catalog-sidebar-width));
}

.article-catalog-sidebar-button-expanded {
	transform: translateX(calc(var(--app-article-catalog-sidebar-width) - 16px));
}

// 目录侧栏中的顶部标题
.article-catalog-sidebar__title {
	margin-bottom: 0.75rem;
	font-size: large;
	font-weight: bold;
	text-align: center;
}

// 目录项
.article-catalog-sidebar__item {
	width: 100%;
	padding-block: 0.3rem;
	border: 0;
	background: transparent;
	font-size: 0.9rem;
	color: var(--app-text-muted);
	text-align: left;
	cursor: pointer;
}

.article-catalog-sidebar__item:hover,
.article-catalog-sidebar__item.is-active {
	color: var(--el-color-primary);
	font-weight: bold;
}

.article-catalog-sidebar__item.is-active {
	border-left: 3px solid var(--app-main);
}
</style>
