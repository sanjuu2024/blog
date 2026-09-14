<template>
	<section
		class="article-filter-panel"
		aria-label="文章筛选"
	>
		<div class="article-filter-panel__header">
			<i-lucide-filter class="article-filter-panel__title-icon" />
			<h2>筛选文章</h2>
		</div>

		<div class="article-filter-panel__controls">
			<div class="article-filter-field">
				<div class="article-filter-field__label">
					<component :is="getRouteIcon('category')" />
					<span>分类</span>
				</div>
				<el-tree-select
					v-model="filterForm.categoryId"
					class="article-filter-field__control"
					:data="categoryList"
					value-key="id"
					placeholder="全部分类"
					aria-label="选择文章分类"
					clearable
					check-strictly
					default-expand-all
					:props="{
						children: 'children',
						label: 'name',
					}"
				/>
			</div>

			<div class="article-filter-field">
				<div class="article-filter-field__label">
					<component :is="getRouteIcon('tag')" />
					<span>标签</span>
				</div>
				<div
					ref="tagSelectContainerRef"
					class="article-filter-field__control"
				>
					<el-select
						v-model="filterForm.tagIds"
						class="article-tag-select"
						placeholder="全部标签"
						aria-label="选择文章标签"
						multiple
						filterable
						clearable
						collapse-tags
						collapse-tags-tooltip
						:max-collapse-tags="maxCollapseTagCount"
						:tag-tooltip="{ placement: 'top' }"
						popper-class="article-tag-select-popper"
					>
						<template #header>
							<div class="article-selected-tags">
								<span class="article-selected-tags__title">
									已选 {{ selectedTagList.length }} 个标签
								</span>
								<div
									v-if="selectedTagList.length"
									class="article-selected-tags__list"
								>
									<el-tag
										v-for="tag in selectedTagList"
										:key="tag.id"
										closable
										@click.stop
										@close="removeSelectedTag(tag.id)"
									>
										{{ tag.name }}
									</el-tag>
								</div>
							</div>
						</template>

						<el-option
							v-for="tag in tagList"
							:key="tag.id"
							:label="tag.name"
							:value="tag.id"
						>
							<span class="article-tag-option">
								<span class="article-tag-option__bullet"></span>
								<span class="article-tag-option__name">{{ tag.name }}</span>
							</span>
						</el-option>
					</el-select>
				</div>
			</div>
		</div>
	</section>
</template>

<script setup lang="ts">
import { useElementSize } from '@vueuse/core';
import { computed, useTemplateRef } from 'vue';
import type { PublicCategoryItem } from '@/modules/category/types/category';
import type { PublicTagItem } from '@/modules/tag/types/tag';
import type { ArticleFilterForm } from '../types/article';
import getRouteIcon from '@/utils/getRouteIcon';

defineOptions({
	name: 'ArticleFilterPanel',
});

// 选择的分类和标签
const filterForm = defineModel<ArticleFilterForm>('filterForm', {
	required: true,
});

// 启用的分类和标签列表
const props = defineProps<{
	categoryList: PublicCategoryItem[];
	tagList: PublicTagItem[];
}>();

const tagSelectContainerRef = useTemplateRef<HTMLElement>('tagSelectContainerRef');
const { width: tagSelectWidth } = useElementSize(tagSelectContainerRef);

// 标签栏越宽，折叠前可以保留的标签越多
const maxCollapseTagCount = computed(() => {
	if (tagSelectWidth.value >= 600) return 5;
	if (tagSelectWidth.value >= 480) return 4;
	if (tagSelectWidth.value >= 360) return 3;
	return 2;
});

const selectedTagList = computed(() => {
	const selectedTagIds = filterForm.value.tagIds ?? [];
	return props.tagList.filter((tag) => selectedTagIds.includes(tag.id));
});

function removeSelectedTag(tagId: number) {
	const selectedTagIds = filterForm.value.tagIds ?? [];
	filterForm.value.tagIds = selectedTagIds.filter((id) => id !== tagId);
}
</script>

<style scoped lang="scss">
.article-filter-panel {
	padding: 1rem;
	border: 1.2px dashed var(--app-border);
	border-radius: 8px;
	background-color: var(--app-surface);
}

.article-filter-panel__header {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	margin-bottom: 0.75rem;
	font-size: 1rem;
	font-weight: 700;
}

.article-filter-panel__title-icon {
	color: var(--app-main);
}

.article-filter-panel__controls {
	display: grid;
	grid-template-columns: minmax(200px, 280px) minmax(260px, 1fr);
	align-items: end;
	gap: 1rem;
}

.article-filter-field {
	min-width: 0;

	.article-filter-field__label {
		display: flex;
		align-items: center;
		gap: 0.375rem;
		margin-bottom: 0.375rem;
		font-size: 0.9rem;
	}
}

.article-filter-field__control {
	width: 100%;
}

.article-tag-select {
	width: 100%;
}

:global(.article-tag-select-popper .el-select-dropdown__list) {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
	gap: 0.25rem 0.5rem;
	padding: 0.375rem;
}

:global(.article-tag-select-popper .el-select-dropdown__item) {
	min-width: 0;
	padding-inline: 0.5rem;
	border-radius: 4px;
}

.article-selected-tags__title {
	display: block;
	margin-bottom: 0.5rem;
	color: var(--app-text-muted);
	font-size: 0.75rem;
}

.article-selected-tags__list {
	display: flex;
	flex-wrap: wrap;
	gap: 0.375rem;
	max-height: 5rem;
	overflow-y: auto;
}

.article-tag-option {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	min-width: 0;
}

.article-tag-option__bullet {
	width: 5px;
	height: 5px;
	border-radius: 50%;
	background-color: var(--app-text-muted);
	flex: 0 0 auto;
}

.article-tag-option__name {
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

:global(.article-tag-select-popper .el-select-dropdown__item.is-selected)
	.article-tag-option__bullet {
	background-color: var(--app-main);
}

:global(.article-tag-select-popper .el-select-dropdown__item.is-selected) {
	background-color: color-mix(in srgb, var(--app-main) 10%, var(--app-surface));
}

@media (width < 768px) {
	.article-filter-panel__controls {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}

@media (width < 600px) {
	.article-filter-panel__controls {
		grid-template-columns: minmax(0, 1fr);
	}
}
</style>
