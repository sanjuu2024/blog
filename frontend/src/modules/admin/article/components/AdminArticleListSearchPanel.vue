<template>
	<div class="admin-article-search-panel">
		<!-- @submit.prevent：阻止表单的原生回车提交，让 el-input 回车调用函数 -->
		<el-form
			label-width="auto"
			label-position="right"
			:model="filterForm"
			@submit.prevent
			class="ml-1"
		>
			<div class="mb-6 flex">
				<!-- 搜索框内按下回车键也触发搜索 -->
				<el-input
					v-model.trim="filterForm.title"
					placeholder="请输入文章标题模糊搜索"
					@keyup.enter="emit('search')"
				/>
				<el-button
					type="primary"
					class="ml-2"
					aria-label="搜索"
					@click="emit('search')"
				>
					<template #icon>
						<i-lets-icons-search-alt />
					</template>
					搜索
				</el-button>

				<el-button
					type="warning"
					class="ml-2"
					aria-label="重置"
					@click="emit('reset')"
				>
					<template #icon>
						<i-lets-icons-refresh />
					</template>
					重置
				</el-button>
			</div>
			<!-- </el-form-item> -->

			<div class="flex">
				<el-form-item
					class="mr-4 w-80"
					label="分类"
				>
					<el-tree-select
						v-model="filterForm.categoryId"
						value-key="id"
						:data="categoryList"
						:props="{
							label: 'name',
						}"
						check-strictly
						placeholder="请选择分类"
						clearable
						default-expand-all
						@change="emit('search')"
					/>
				</el-form-item>
			</div>

			<el-form-item label="状态">
				<el-radio-group
					v-model="filterForm.status"
					@change="emit('search')"
				>
					<el-radio :value="'ALL'">全部</el-radio>
					<el-radio :value="ARTICLE_STATUS.DRAFT">草稿</el-radio>
					<el-radio :value="ARTICLE_STATUS.PUBLISHED">已发布</el-radio>
					<el-radio :value="ARTICLE_STATUS.OFFLINE">下线</el-radio>
				</el-radio-group>
			</el-form-item>
		</el-form>
	</div>
</template>

<script setup lang="ts">
import { ARTICLE_STATUS, type AdminArticleFilterForm } from '../types/adminArticle';
import type { AdminCategoryItem } from '../../category/types/adminCategory';

const filterForm = defineModel<AdminArticleFilterForm>('filterForm', {
	required: true,
});

defineProps<{
	categoryList: AdminCategoryItem[];
}>();

const emit = defineEmits<{
	search: [];
	reset: [];
}>();

defineOptions({
	name: 'AdminArticleListSearchPanel',
});
</script>

<style></style>
