<template>
	<div class="admin-article">
		<el-card class="admin-article-list-card">
			<template #header>
				<AdminArticleHeader />
			</template>

			<!-- 🔺🔺🔺注意 template 中用羊肉串写法，子组件接收用小驼峰写法，可以自动转换！ -->
			<AdminArticleSearchPanel
				v-model:filter-form="filterForm"
				:category-list="categoryList"
				@search="getArticleList"
				@reset="resetFilterForm"
			/>

			<!-- 🔺🔺🔺用 v-bind 还是 v-model，取决于：子组件需不需要修改这个值 -->
			<AdminArticleList
				:article-list="articleList"
				:page-meta="pageMeta"
				v-model:page-num="pageParams.pageNum"
				v-model:page-size="pageParams.pageSize"
				@delete="clickDeleteArticle"
				@update-status="clickUpdateArticleStatus"
				@page-change="getArticleList"
			/>
		</el-card>
	</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import AdminArticleHeader from '../components/AdminArticleListHeader.vue';
import AdminArticleSearchPanel from '../components/AdminArticleListSearchPanel.vue';
import AdminArticleList from '../components/AdminArticleListTable.vue';
import { useAdminCategoryList } from '../../category/composables/useAdminCategoryList';
import { useAdminArticleList } from '../composables/useAdminArticleList';
import { useAdminArticleActions } from '../composables/useAdminArticleActions.ts';
import { ARTICLE_STATUS, type AdminArticleListItem } from '../types/adminArticle.ts';

const { articleList, pageMeta, pageParams, filterForm, getArticleList, resetFilterForm } =
	useAdminArticleList();

const { handleDeleteArticle, handleUpdateArticleStatus } = useAdminArticleActions();

const { categoryList, getCategoryList } = useAdminCategoryList();

defineOptions({
	name: 'AdminArticleListPage',
});

onMounted(() => {
	getCategoryList();
	getArticleList();
});

// 点击 删除 按钮
async function clickDeleteArticle(id: number) {
	try {
		// 删除成功后刷新文章列表
		await handleDeleteArticle(id);
		getArticleList();
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	}
}

// 点击 修改文章状态 按钮，已发布状态可以改为下线状态，其他状态可以改为已发布状态
async function clickUpdateArticleStatus(row: AdminArticleListItem) {
	try {
		const data = await handleUpdateArticleStatus(
			row.id,
			row.status === ARTICLE_STATUS.PUBLISHED
				? ARTICLE_STATUS.OFFLINE
				: ARTICLE_STATUS.PUBLISHED,
		);
		if (data) {
			row.status = data.status;
			row.updatedAt = data.updatedAt;
		}
	} catch {
		// 错误提示已经由 request 响应拦截器统一处理
	}
}
</script>

<style scoped lang="scss">
.admin-article {
	height: 100%;
	min-height: 0;
}

.admin-article-list-card {
	display: flex;
	height: 100%;
	flex-direction: column;
	box-shadow: none;
	border: none;
}

.admin-article-list-card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}
</style>
