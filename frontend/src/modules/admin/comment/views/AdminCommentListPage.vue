<template>
	<div class="admin-comment">
		<el-card class="admin-comment-list-card">
			<template #header>
				<div class="header flex items-center justify-between">
					<span class="text-xl font-bold">评论管理</span>
				</div>
			</template>

			<template #default>
				<!-- @submit.prevent：阻止表单的原生回车提交 -->
				<el-form
					label-width="auto"
					label-position="right"
					:model="filterForm"
					@submit.prevent
					class="ml-1"
				>
					<div class="admin-comment__search">
						<el-form-item
							prop="articleId"
							label="文章 ID"
						>
							<el-input-number
								v-model="filterForm.articleId"
								:min="1"
								:controls="false"
								clearable
								placeholder="请输入文章 ID"
								@keyup.enter="getCommentList(1)"
								align="left"
							/>
						</el-form-item>
						<el-form-item
							prop="userId"
							label="用户 ID"
						>
							<el-input-number
								v-model="filterForm.userId"
								:min="1"
								:controls="false"
								clearable
								placeholder="请输入用户 ID"
								@keyup.enter="getCommentList(1)"
								align="left"
							/>
						</el-form-item>
						<div class="admin-comment__search-actions">
							<el-button
								type="primary"
								aria-label="搜索"
								@click="getCommentList(1)"
							>
								<template #icon>
									<i-lets-icons-search-alt />
								</template>
								搜索
							</el-button>

							<el-button
								type="warning"
								aria-label="重置"
								@click="resetFilterForm"
							>
								<template #icon>
									<i-lets-icons-refresh />
								</template>
								重置
							</el-button>
						</div>
					</div>

					<el-form-item
						prop="createdAtRange"
						label="评论时间"
					>
						<el-date-picker
							v-model="filterForm.createdAtRange"
							type="datetimerange"
							range-separator="至"
							start-placeholder="开始时间"
							end-placeholder="结束时间"
							value-format="YYYY-MM-DDTHH:mm:ssZ"
							@change="getCommentList(1)"
							:default-time="defaultTime"
						/>
					</el-form-item>

					<div class="flex">
						<el-form-item
							prop="status"
							label="评论状态"
						>
							<el-radio-group
								v-model="filterForm.status"
								@change="getCommentList(1)"
							>
								<el-radio value="">全部</el-radio>
								<el-radio :value="COMMENT_STATUS.PENDING">待审核</el-radio>
								<el-radio :value="COMMENT_STATUS.APPROVED">已通过</el-radio>
								<el-radio :value="COMMENT_STATUS.REJECTED">已拒绝</el-radio>
								<el-radio :value="COMMENT_STATUS.HIDDEN">已隐藏</el-radio>
								<el-radio :value="COMMENT_STATUS.DELETED">已删除</el-radio>
							</el-radio-group>
						</el-form-item>
					</div>

					<div class="flex">
						<el-form-item
							prop="type"
							label="评论层级"
						>
							<el-radio-group
								v-model="filterForm.type"
								@change="getCommentList(1)"
							>
								<el-radio value="">全部</el-radio>
								<el-radio :value="COMMENT_TYPE.TOP_LEVEL">顶层评论</el-radio>
								<el-radio :value="COMMENT_TYPE.REPLY">回复</el-radio>
							</el-radio-group>
						</el-form-item>
					</div>
				</el-form>

				<!-- 评论列表 -->
				<div class="admin-comment-table-wrap">
					<el-table
						ref="commentTableRef"
						:data="commentList"
						row-key="id"
						:row-class-name="getCommentRowClassName"
						class="admin-comment-table"
						height="100%"
					>
						<el-table-column
							prop="id"
							align="center"
							width="90"
							label="ID"
						/>

						<el-table-column
							label="评论内容"
							min-width="260"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								<el-tooltip
									:content="row.content"
									placement="top-start"
									effect="light"
								>
									<p class="admin-comment-content">{{ row.content }}</p>
								</el-tooltip>
							</template>
						</el-table-column>

						<el-table-column
							label="所属文章"
							align="center"
							min-width="180"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								<RouterLink
									v-if="row.article"
									:to="`/admin/articles/${row.article.id}/edit`"
									class="font-bold"
								>
									{{ row.article.title }}
								</RouterLink>
								<p
									v-if="row.article"
									class="text-xs"
								>
									ID: {{ row.article.id }}
								</p>
								<span v-else>-</span>
							</template>
						</el-table-column>

						<el-table-column
							label="评论用户"
							align="center"
							width="130"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								<div v-if="row.author">
									<p>{{ row.author.nickname || row.author.username }}</p>
									<p class="text-xs">ID: {{ row.author.id }}</p>
								</div>
								<span v-else>-</span>
							</template>
						</el-table-column>

						<el-table-column
							label="层级"
							align="center"
							width="100"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								<el-tag
									type="info"
									effect="plain"
								>
									{{ getCommentTypeLabel(row.type) }}
								</el-tag>
							</template>
						</el-table-column>

						<el-table-column
							label="状态"
							align="center"
							width="100"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								<el-tag
									:type="getCommentStatusTagType(row.status)"
									effect="plain"
									class="font-bold"
									:class="{
										'comment-status-rejected':
											row.status === COMMENT_STATUS.REJECTED,
									}"
								>
									{{ getCommentStatusLabel(row.status) }}
								</el-tag>
							</template>
						</el-table-column>

						<el-table-column
							label="处理原因"
							align="center"
							min-width="160"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								{{ row.moderationReason || '-' }}
							</template>
						</el-table-column>

						<el-table-column
							label="创建时间"
							align="center"
							width="180"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								{{ formatDateTime(row.createdAt) }}
							</template>
						</el-table-column>

						<el-table-column
							label="处理时间"
							align="center"
							width="180"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								{{
									formatDateTime(
										row.status === COMMENT_STATUS.DELETED
											? row.deletedAt
											: row.reviewedAt,
									)
								}}
							</template>
						</el-table-column>

						<el-table-column
							label="评论操作"
							align="center"
							width="300"
							:fixed="isMobile ? false : 'right'"
						>
							<template #default="{ row }: { row: AdminCommentItem }">
								<el-popconfirm
									v-if="canApprove(row)"
									:title="`确认通过评论 ${row.id} 吗？`"
									@confirm="approveComment(row)"
								>
									<template #reference>
										<el-button
											type="success"
											title="通过评论"
										>
											<template #icon>
												<i-lets-icons-check-fill />
											</template>
											通过
										</el-button>
									</template>
								</el-popconfirm>

								<el-button
									v-if="canReject(row)"
									type="warning"
									title="拒绝评论"
									@click="rejectComment(row)"
								>
									<template #icon>
										<i-lets-icons-cancel />
									</template>
									拒绝
								</el-button>

								<el-button
									v-if="canHide(row)"
									type="warning"
									title="隐藏评论"
									@click="hideComment(row)"
								>
									<template #icon>
										<i-lucide-eye-off />
									</template>
									隐藏
								</el-button>

								<el-button
									v-if="canDelete(row)"
									type="danger"
									title="删除评论"
									@click="deleteComment(row)"
								>
									<template #icon>
										<i-ep-delete />
									</template>
									删除
								</el-button>

								<span
									v-if="
										!canApprove(row) &&
										!canReject(row) &&
										!canHide(row) &&
										!canDelete(row)
									"
								>
									-
								</span>
							</template>
						</el-table-column>
					</el-table>
				</div>

				<!-- 分页部分 -->
				<!-- 注意 current-page 和 page-size 是 v-model 双向绑定，而不只是 v-bind -->
				<el-pagination
					v-model:current-page="pageParams.pageNum"
					v-model:page-size="pageParams.pageSize"
					:background="true"
					:layout="
						isMobile
							? 'prev, next, jumper, total'
							: 'prev, pager, next, jumper, ->, sizes, total'
					"
					:total="pageMeta.total"
					:page-sizes="[3, 5, 7, 9]"
					@current-change="getCommentList"
					@size-change="getCommentList(1)"
					class="admin-comment-pagination"
				/>
			</template>
		</el-card>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import type { TableInstance } from 'element-plus';
import { formatDateTime } from '@/utils/datetime';
import { useAdminCommentList } from '../composables/useAdminCommentList';
import {
	COMMENT_STATUS,
	COMMENT_TYPE,
	type AdminCommentItem,
	type CommentStatus,
	type CommentType,
} from '../types/adminComment';
import { useMediaQuery } from '@vueuse/core';

const isMobile = useMediaQuery('(width < 768px)');

defineOptions({
	name: 'AdminCommentListPage',
});

const {
	commentList,
	pageMeta,
	pageParams,
	filterForm,
	getCommentList,
	resetFilterForm,
	approveComment,
	rejectComment,
	hideComment,
	deleteComment,
	canApprove,
	canReject,
	canHide,
	canDelete,
} = useAdminCommentList();

const commentTableRef = ref<TableInstance>();

const defaultTime: [Date, Date] = [new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]; // '00:00:00', '23:59:59'

onMounted(() => {
	getCommentList();
});

watch(commentList, async () => {
	await nextTick();
	commentTableRef.value?.setScrollTop(0);
});

// 获取评论状态展示文案。
function getCommentStatusLabel(status: CommentStatus) {
	switch (status) {
		case COMMENT_STATUS.PENDING:
			return '待审核';
		case COMMENT_STATUS.APPROVED:
			return '已通过';
		case COMMENT_STATUS.REJECTED:
			return '已拒绝';
		case COMMENT_STATUS.HIDDEN:
			return '已隐藏';
		default:
			return '已删除';
	}
}

// 获取评论状态标签类型。
function getCommentStatusTagType(status: CommentStatus) {
	switch (status) {
		case COMMENT_STATUS.APPROVED:
			return 'success';
		case COMMENT_STATUS.PENDING:
			return 'warning';
		case COMMENT_STATUS.REJECTED:
		case COMMENT_STATUS.DELETED:
			return 'danger';
		default:
			return 'info';
	}
}

// 获取评论层级展示文案。
function getCommentTypeLabel(type: CommentType) {
	return type === COMMENT_TYPE.TOP_LEVEL ? '顶层评论' : '回复';
}

// 获取已处理评论的灰度行样式。
function getCommentRowClassName({ row }: { row: AdminCommentItem }) {
	return row.status === COMMENT_STATUS.DELETED ||
		row.status === COMMENT_STATUS.REJECTED ||
		row.status === COMMENT_STATUS.HIDDEN
		? 'comment-row-muted'
		: '';
}
</script>

<style scoped lang="scss">
.admin-comment {
	height: 100%;
	min-height: 0;
}

.admin-comment-list-card {
	display: flex;
	height: 100%;
	flex-direction: column;
	box-shadow: none;
	border: none;
	background-color: var(--app-bg);
}

.admin-comment-list-card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-comment-table-wrap {
	min-height: 0;
	flex: 1;
}

.admin-comment__search {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
	align-items: flex-start;
	gap: 0.5rem;

	:deep(.el-input-number) {
		width: 100%;
		min-width: 0;
	}

	.admin-comment__search-actions {
		display: flex;
		gap: 0.5rem;
		margin-bottom: 1rem;
	}

	// Element Plus 默认给相邻按钮添加了左边距，记得去掉，否则相邻两个按钮除了 grid 布局的 gap 之外还会有额外的间距
	:deep(.el-button) {
		margin: 0;
	}

	@media (width < 768px) {
		grid-template-columns: repeat(2, minmax(0, 1fr));

		.admin-comment__search-actions {
			grid-column: 1 / -1;

			.el-button {
				flex: 1;
			}
		}
	}

	@media (width < 400px) {
		grid-template-columns: minmax(0, 1fr);
	}
}

// 自定义表格样式，覆盖 Element Plus 默认的行 hover
.admin-comment-table {
	--el-table-row-hover-bg-color: var(--app-table-row-hover-bg-color);

	height: 100%;
}

.admin-comment-table :deep(.comment-row-muted) {
	--el-table-text-color: var(--app-text-muted);

	color: var(--app-text-muted);
}

.admin-comment-table :deep(.comment-row-muted td),
.admin-comment-table :deep(.comment-row-muted .cell),
.admin-comment-table :deep(.comment-row-muted a),
.admin-comment-table :deep(.comment-row-muted p) {
	color: var(--app-text-muted);
}

.admin-comment-content {
	display: -webkit-box;
	overflow: hidden;
	-webkit-line-clamp: 2;
	line-clamp: 2;
	-webkit-box-orient: vertical;
	line-height: 1.5;
}

.admin-comment-pagination {
	margin-top: 1.5rem;
}
</style>
