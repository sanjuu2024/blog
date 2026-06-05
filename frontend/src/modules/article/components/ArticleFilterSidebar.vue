<template>
	<Teleport to="body">
		<button
			class="article-filter-sidebar-button"
			:class="{ 'article-filter-sidebar-button-expanded': expanded }"
			@click="expanded = !expanded"
			aria-label="打开/关闭文章筛选侧栏"
		>
			<i-lucide-filter />
		</button>

		<aside
			class="article-filter-sidebar"
			:class="{ 'article-filter-sidebar-expanded': expanded }"
		>
			<div class="article-filter-sidebar__inner">
				<div class="article-filter-sidebar__header">
					<h2 class="font-bold">筛选文章</h2>
				</div>
				<div class="article-filter-sidebar__main">
					<div class="article-filter-sidebar-form">
						<section
							label="分类"
							class="font-bold"
						>
							<div
								class="article-filter-sidebar-form-category my-4 flex items-center"
							>
								<component
									:is="getRouteIcon('category')"
									class="mr-2 text-(--app-button-bg)"
								/>
								<span>选择文章分类</span>
							</div>
							<el-tree-select
								:data="categoryList"
								v-model="filterForm.categoryId"
								value-key="id"
								placeholder="选择分类"
								clearable
								check-strictly
								default-expand-all
								popper-class="article-category-tree-popper"
								:props="{
									children: 'children',
									label: 'name',
								}"
								@visible-change="categorySelectVisible = $event"
							/>
						</section>

						<section
							label="标签"
							class="font-bold"
						>
							<div class="article-filter-sidebar-form-tag my-4 flex items-center">
								<component
									:is="getRouteIcon('tag')"
									class="mr-2 text-(--app-button-bg)"
								/>
								<span>选择文章标签</span>
							</div>
							<div class="article-checkbox-div">
								<el-checkbox-group
									class="article-tag-checkbox-group"
									v-model="filterForm.tagIds"
								>
									<el-checkbox
										v-for="tag in tagList"
										:key="tag.id"
										:value="tag.id"
									>
										{{ tag.name }}
									</el-checkbox>
								</el-checkbox-group>
							</div>
						</section>
					</div>
				</div>

				<div class="article-filter-sidebar__footer w-full">
					<el-button
						type="warning"
						dashed
						class="mt-4 mb-2 w-full"
						@click="emit('resetFilterForm')"
						>重置</el-button
					>
					<el-button
						type="default"
						class="my-2 w-full"
						@click="applyFilter"
						>应用筛选条件</el-button
					>
				</div>
			</div>
		</aside>
	</Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { PublicCategoryItem } from '@/modules/category/types/category';
import type { PublicTagItem } from '@/modules/tag/types/tag';
import type { ArticleFilterForm } from '../types/article';
import { useEventListener } from '@vueuse/core';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'ArticleFilterSidebar',
});

// 控制拉伸
const expanded = ref(false);
// 标识分类下拉框是否展开中
const categorySelectVisible = ref(false);

// 选择的分类和标签
const filterForm = defineModel<ArticleFilterForm>('filterForm', {
	required: true,
});

// 启用的分类和标签列表
defineProps<{
	categoryList: PublicCategoryItem[];
	tagList: PublicTagItem[];
}>();

// 重置分页函数
const emit = defineEmits<{
	resetFilterForm: [];
	getList: [];
}>();

function applyFilter() {
	expanded.value = false;
	emit('getList');
}

// 使用 vueuse 的监听器（自动清理），侧边栏展开时键盘按 esc 键关闭侧栏
useEventListener(
	document,
	'keydown',
	(event: KeyboardEvent) => {
		if (event.key !== 'Escape' || !expanded.value) {
			return;
		}

		// 第一次 esc 交给 el-tree-select 自己去关闭下拉框；
		if (categorySelectVisible.value) return;

		// 第二次 esc 关闭侧边栏
		expanded.value = false;
	},
	{
		capture: true, // 🔺🔺🔺先捕获再冒泡，确保在 el-tree-select 之前触发，避免 el-tree-select 捕获之后不冒泡
	},
);
</script>

<style scoped lang="scss">
.article-filter-sidebar-button {
	position: fixed;
	top: var(--app-header-height);
	left: 0;
	width: 40px;
	height: 40px;
	border-radius: 0 0 16px 0;
	background-color: var(--app-button-bg);
	color: white;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
	transition:
		transform 0.2s ease,
		background-color 0.1s ease;
	z-index: 999;
	cursor: pointer;

	// &:hover {
	//     background-color: var(--app-button-bg-hover);
	// }

	&:focus {
		outline: none;
	}

	&:focus-visible {
		outline: 2px solid rgba(88, 127, 92, 0.35);
		outline-offset: 2px;
	}
}

.article-filter-sidebar-button-expanded {
	transform: translateX(var(--app-article-sidebar-width));
}

.article-filter-sidebar {
	position: fixed;
	top: var(--app-header-height);
	left: calc(-1 * var(--app-article-sidebar-width));
	width: var(--app-article-sidebar-width);
	background-color: var(--app-surface-muted);
	box-shadow: 2px 4px 4px rgba(0, 0, 0, 0.1);
	border-radius: 0 0 1rem 0;
	transition:
		transform 0.2s ease,
		background-color 0.1s ease;
	z-index: 999;
}

.article-filter-sidebar-expanded {
	transform: translateX(var(--app-article-sidebar-width));
}

.article-filter-sidebar__inner {
	padding: 1.5rem;
}

.article-filter-sidebar__header {
	display: flex;
	align-items: center;
	justify-content: center;
	text-align: center;
	font-size: large;
	margin-bottom: 1rem;
}

.article-filter-sidebar__footer {
	:deep(.el-button) {
		margin-left: 0;
	}
}

.article-filter-sidebar {
	:deep(.el-checkbox__inner),
	:deep(.el-checkbox__inner::after) {
		transition-duration: 0ms;
		transition-delay: 0s;
	}
}

:global(.article-category-tree-popper .el-tree-node__content) {
	min-height: 36px;
	border-bottom: 1px solid var(--app-border);
}

.article-checkbox-div {
	width: 100%;
	max-height: 300px;
	overflow-y: auto;

	&::-webkit-scrollbar {
		width: 7px;
	}

	&::-webkit-scrollbar-track {
		background: transparent;
	}

	&::-webkit-scrollbar-thumb {
		border-radius: 9999px;
		background-color: rgba(31, 41, 51, 0.28);
		// background-color: var(--el-color-warning-light-7);
	}

	&::-webkit-scrollbar-thumb:hover {
		background-color: rgba(31, 41, 51, 0.45);
		// background-color: var(--el-color-warning-light-5);
	}
}

.article-tag-checkbox-group {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 8px 12px;

	:deep(.el-checkbox) {
		margin-right: 0;
		min-width: 0;
	}

	:deep(.el-checkbox__label) {
		min-width: 0;
		line-height: 1.5rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
}
</style>
