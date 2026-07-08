<template>
	<div class="admin-article-list">
		<!-- 文章列表 -->
		<div class="admin-article-table-wrap">
			<el-table
				ref="articleTableRef"
				:data="articleList"
				row-key="id"
				class="admin-article-table"
				height="100%"
			>
				<el-table-column
					align="center"
					width="30"
					prop="isTop"
				>
					<template #default="{ row }">
						<div class="admin-article-is-top">
							<i-solar-pin-bold
								v-if="row.isTop"
								style="color: var(--app-button-bg)"
							/>
							<i-solar-pin-outline v-else />
						</div>
					</template>
				</el-table-column>
				<el-table-column
					align="center"
					label="总览"
				>
					<template #default="{ row }: { row: AdminArticleListItem }">
						<div class="flex items-center">
							<RouterLink
								:to="`/admin/articles/${row.id}/edit`"
								class="flex items-center font-bold"
							>
								<AppImage
									:url="row.coverUrl"
									alt="文章封面图"
									class="mr-4 h-20 w-20 shrink-0 rounded border border-(--app-border)"
								/>
								<span class="mx-4 text-left">{{ row.title }}</span>
							</RouterLink>
						</div>
					</template>
				</el-table-column>

				<el-table-column
					align="center"
					width="100"
					label="分类"
				>
					<template #default="{ row }: { row: AdminArticleListItem }">
						{{ row.category.parent.name }} - {{ row.category.name }}
					</template>
				</el-table-column>

				<el-table-column
					label="状态"
					align="center"
					width="100"
				>
					<template #default="{ row }: { row: AdminArticleListItem }">
						<div
							v-if="row.status == ARTICLE_STATUS.DRAFT"
							class="text-center"
						>
							<div class="flex items-center justify-center">
								<i-lets-icons-edit class="text-xl text-yellow-600" />
							</div>
							<p>草稿</p>
						</div>
						<div
							v-else-if="row.status == ARTICLE_STATUS.PUBLISHED"
							class="text-center"
						>
							<div class="flex items-center justify-center">
								<i-lets-icons-file-dock class="text-xl text-green-600" />
							</div>
							<p>已发布</p>
						</div>
						<div
							v-else
							class="text-center"
						>
							<div class="flex items-center justify-center">
								<i-lets-icons-lock class="text-xl text-gray-400" />
							</div>
							<p>下线</p>
						</div>
					</template>
				</el-table-column>

				<el-table-column
					align="center"
					width="180"
					label="发表时间"
				>
					<template #default="{ row }: { row: AdminArticleListItem }">
						{{ row.publishedAt ? formatDateTime(row.publishedAt) : '-' }}
					</template>
				</el-table-column>

				<el-table-column
					align="center"
					width="180"
					label="最近编辑时间"
				>
					<template #default="{ row }: { row: AdminArticleListItem }">
						{{ row.updatedAt ? formatDateTime(row.updatedAt) : '-' }}
					</template>
				</el-table-column>

				<el-table-column
					label="文章操作"
					align="center"
					width="320"
					fixed="right"
				>
					<!-- 已发布状态可以改为下线状态，其他状态可以改为已发布状态 -->
					<template #default="{ row }: { row: AdminArticleListItem }">
						<el-button
							type="warning"
							title="编辑文章"
							@click="router.push(`/admin/articles/${row.id}/edit`)"
						>
							<template #icon>
								<i-ep-edit />
							</template>
							编辑
						</el-button>

						<el-popconfirm
							:hide-icon="true"
							:hide-after="0"
							:title="`确认${row.status === ARTICLE_STATUS.DRAFT ? '发布' : row.status === ARTICLE_STATUS.PUBLISHED ? '下线' : '重新发布'} ${row.title}？`"
							@confirm="emit('updateStatus', row)"
						>
							<template #reference>
								<el-button
									:type="
										row.status === ARTICLE_STATUS.DRAFT
											? 'success'
											: row.status === ARTICLE_STATUS.PUBLISHED
												? 'danger'
												: 'warning'
									"
									:title="
										row.status === ARTICLE_STATUS.DRAFT
											? '发布'
											: row.status === ARTICLE_STATUS.PUBLISHED
												? '下线'
												: '重新发布'
									"
								>
									<template #icon>
										<i-ep-upload v-if="row.status === ARTICLE_STATUS.DRAFT" />
										<i-ep-download
											v-else-if="row.status === ARTICLE_STATUS.PUBLISHED"
										/>
										<i-ep-refresh v-else />
									</template>
									{{
										row.status === ARTICLE_STATUS.DRAFT
											? '发布'
											: row.status === ARTICLE_STATUS.PUBLISHED
												? '下线'
												: '重新发布'
									}}
								</el-button>
							</template>
						</el-popconfirm>

						<!-- @confirm="clickDeleteArticle(row.id)" -->
						<el-popconfirm
							:title="`确认删除文章 ${row.title} 吗？`"
							@confirm="emit('delete', row.id)"
						>
							<template #reference>
								<el-button
									type="danger"
									title="删除文章"
								>
									<template #icon>
										<i-ep-delete />
									</template>
									删除
								</el-button>
							</template>
						</el-popconfirm>
					</template>
				</el-table-column>
			</el-table>
		</div>

		<!-- 分页部分 -->
		<!-- 注意 current-page 和 page-size 是 v-model 双向绑定，而不只是 v-bind -->
		<el-pagination
			v-model:current-page="pageNum"
			v-model:page-size="pageSize"
			:background="true"
			layout="prev, pager, next, jumper, ->, sizes, total"
			:total="pageMeta.total"
			:page-sizes="[3, 5, 7, 9]"
			@current-change="emit('pageChange')"
			@size-change="emit('pageChange')"
			class="admin-article-pagination"
		/>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onActivated, ref, watch } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import { useRouter } from 'vue-router';
import type { TableInstance } from 'element-plus';
import {
	ARTICLE_STATUS,
	type AdminArticleListItem,
	type AdminArticlePageMeta,
} from '../types/adminArticle';
import AppImage from '@/components/AppImage.vue';

const router = useRouter();

defineOptions({
	name: 'AdminArticleListTable',
});

// 注意 pageNum 和 pageSize 是通过 v-model 双向绑定的，所以用 defineModel，而不是 defineProps
const pageNum = defineModel<number>('pageNum', { required: true });
const pageSize = defineModel<number>('pageSize', { required: true });

const props = defineProps<{
	articleList: AdminArticleListItem[];
	pageMeta: AdminArticlePageMeta;
}>();

const articleTableRef = ref<TableInstance>();

const emit = defineEmits<{
	delete: [number];
	updateStatus: [AdminArticleListItem];
	pageChange: [];
}>();

watch(
	() => props.articleList,
	async () => {
		await nextTick();
		articleTableRef.value?.setScrollTop(0);
	},
);

onActivated(() => {
	// 并且重新拉取可能更新后的文章列表数据
	emit('pageChange');
});
</script>

<style scoped lang="scss">
.admin-article-list {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-article-table-wrap {
	min-height: 0;
	flex: 1;
}

// 自定义表格样式，覆盖 Element Plus 默认的行 hover
.admin-article-table {
	--el-table-row-hover-bg-color: var(--app-table-row-hover-bg-color);
	height: 100%;
}

.admin-article-pagination {
	flex: none;
	margin-top: 1.5rem;
}
</style>
