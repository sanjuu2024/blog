<template>
	<div class="admin-message">
		<el-card class="admin-message__card">
			<template #header>
				<div class="flex items-center justify-between">
					<span class="text-xl font-bold">留言管理</span>
					<el-button
						type="success"
						:loading="submitting"
						:disabled="!selectedMessageIds.length || submitting"
						@click="approveSelected"
					>
						<i-lets-icons-check-fill />
						批量通过（{{ selectedMessageIds.length }}）
					</el-button>
				</div>
			</template>

			<el-form
				:model="filterForm"
				label-width="auto"
				label-position="right"
				@submit.prevent
			>
				<div class="admin-message__filters">
					<el-form-item
						prop="messageId"
						label="留言 ID"
					>
						<el-input-number
							v-model="filterForm.messageId"
							:min="1"
							:controls="false"
							placeholder="请输入留言 ID"
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
							placeholder="请输入用户 ID"
							align="left"
						/>
					</el-form-item>
					<el-form-item
						prop="guestNickname"
						label="游客昵称"
					>
						<el-input
							v-model="filterForm.guestNickname"
							clearable
							placeholder="请输入游客昵称"
						/>
					</el-form-item>
					<el-form-item
						prop="guestEmail"
						label="游客邮箱"
					>
						<el-input
							v-model="filterForm.guestEmail"
							clearable
							placeholder="请输入游客邮箱"
						/>
					</el-form-item>
					<el-form-item
						prop="content"
						label="留言内容"
					>
						<el-input
							v-model="filterForm.content"
							clearable
							placeholder="请输入留言内容"
							@keyup.enter="getMessageList(1)"
						/>
					</el-form-item>
					<div class="admin-message__filter-actions">
						<el-button
							type="primary"
							@click="getMessageList(1)"
						>
							<i-lets-icons-search-alt />
							搜索
						</el-button>
						<el-button
							type="warning"
							@click="resetFilterForm"
						>
							<i-lets-icons-refresh />
							重置
						</el-button>
					</div>
				</div>

				<el-form-item
					prop="createdAtRange"
					label="创建时间"
				>
					<el-date-picker
						v-model="filterForm.createdAtRange"
						type="datetimerange"
						range-separator="至"
						start-placeholder="开始时间"
						end-placeholder="结束时间"
						value-format="YYYY-MM-DDTHH:mm:ssZ"
						@change="getMessageList(1)"
					/>
				</el-form-item>

				<el-form-item
					prop="status"
					label="留言状态"
				>
					<el-radio-group
						v-model="filterForm.status"
						@change="getMessageList(1)"
					>
						<el-radio value="">全部</el-radio>
						<el-radio :value="MESSAGE_STATUS.PENDING">待审核</el-radio>
						<el-radio :value="MESSAGE_STATUS.APPROVED">已通过</el-radio>
						<el-radio :value="MESSAGE_STATUS.REJECTED">已拒绝</el-radio>
						<el-radio :value="MESSAGE_STATUS.HIDDEN">已隐藏</el-radio>
						<el-radio :value="MESSAGE_STATUS.DELETED">已删除</el-radio>
					</el-radio-group>
				</el-form-item>

				<el-form-item
					prop="type"
					label="留言层级"
				>
					<el-radio-group
						v-model="filterForm.type"
						@change="getMessageList(1)"
					>
						<el-radio value="">全部</el-radio>
						<el-radio :value="MESSAGE_TYPE.TOP_LEVEL">顶层留言</el-radio>
						<el-radio :value="MESSAGE_TYPE.REPLY">管理员回复</el-radio>
					</el-radio-group>
				</el-form-item>
			</el-form>

			<div class="admin-message__table-wrap">
				<el-table
					ref="tableRef"
					:data="messageList"
					row-key="id"
					height="100%"
					:row-class-name="getRowClassName"
					@selection-change="handleSelectionChange"
				>
					<el-table-column
						type="selection"
						width="48"
						:selectable="isBatchSelectable"
					/>
					<el-table-column
						prop="id"
						label="ID"
						align="center"
						width="90"
					/>
					<el-table-column
						label="留言内容"
						min-width="260"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							<p class="admin-message__content">{{ row.content }}</p>
						</template>
					</el-table-column>
					<el-table-column
						label="留言身份"
						width="180"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							<strong>{{ row.author?.nickname || row.nickname }}</strong>
							<p class="admin-message__secondary">
								{{ row.userId ? `用户 ID: ${row.userId}` : '游客' }}
							</p>
							<p class="admin-message__secondary">{{ row.email || '未填写邮箱' }}</p>
						</template>
					</el-table-column>
					<el-table-column
						label="父留言 ID"
						align="center"
						width="110"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							{{ row.parentId || '-' }}
						</template>
					</el-table-column>
					<el-table-column
						label="通知"
						align="center"
						width="90"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							<div class="admin-message__notify-icon">
								<i-lucide-bell-check
									class="text-(--app-icon-green-color)"
									v-if="row.notifyOnReply"
								/>
								<i-lucide-bell-off
									class="text-(--app-icon-gray-color)"
									v-else
								/>
							</div>
						</template>
					</el-table-column>
					<el-table-column
						label="层级"
						align="center"
						width="110"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							<el-tag
								type="info"
								effect="plain"
							>
								{{ row.type === MESSAGE_TYPE.TOP_LEVEL ? '顶层留言' : '回复' }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column
						label="状态"
						align="center"
						width="100"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							<el-tag
								:type="getStatusTagType(row.status)"
								effect="plain"
								class="font-bold"
								:class="{
									'message-status-rejected':
										row.status === MESSAGE_STATUS.REJECTED,
								}"
							>
								{{ getStatusLabel(row.status) }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column
						label="处理原因"
						min-width="150"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							{{ row.moderationReason || '-' }}
						</template>
					</el-table-column>
					<el-table-column
						label="创建时间"
						align="center"
						width="180"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							{{ formatDateTime(row.createdAt) }}
						</template>
					</el-table-column>
					<el-table-column
						label="处理时间"
						align="center"
						width="180"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							{{
								formatDateTime(
									row.status === MESSAGE_STATUS.DELETED
										? row.deletedAt
										: row.reviewedAt,
								)
							}}
						</template>
					</el-table-column>
					<el-table-column
						label="操作"
						align="center"
						width="360"
						:fixed="isMobile ? false : 'right'"
					>
						<template #default="{ row }: { row: AdminMessageItem }">
							<el-button
								v-if="canApprove(row)"
								type="success"
								:disabled="submitting"
								@click="handleModerate(row, MESSAGE_MODERATION_ACTION.APPROVE)"
							>
								通过
							</el-button>
							<el-button
								v-if="canReply(row)"
								type="primary"
								:disabled="submitting"
								@click="openReplyDialog(row)"
							>
								回复
							</el-button>
							<el-button
								v-if="canReject(row)"
								type="warning"
								:disabled="submitting"
								@click="handleModerate(row, MESSAGE_MODERATION_ACTION.REJECT)"
							>
								拒绝
							</el-button>
							<el-button
								v-if="canHide(row)"
								type="warning"
								:disabled="submitting"
								@click="handleModerate(row, MESSAGE_MODERATION_ACTION.HIDE)"
							>
								隐藏
							</el-button>
							<el-button
								v-if="canDelete(row)"
								type="danger"
								:disabled="submitting"
								@click="handleModerate(row, MESSAGE_MODERATION_ACTION.DELETE)"
							>
								删除
							</el-button>
							<span
								v-if="
									!canApprove(row) &&
									!canReply(row) &&
									!canReject(row) &&
									!canHide(row) &&
									!canDelete(row)
								"
								>-</span
							>
						</template>
					</el-table-column>
				</el-table>
			</div>

			<el-pagination
				v-model:current-page="pageParams.pageNum"
				v-model:page-size="pageParams.pageSize"
				background
				:layout="
					isMobile
						? 'prev, next, jumper, total'
						: 'prev, pager, next, jumper, ->, sizes, total'
				"
				:total="pageMeta.total"
				:page-sizes="[5, 10, 20, 50]"
				@current-change="getMessageList"
				@size-change="getMessageList(1)"
				class="admin-message__pagination"
			/>
		</el-card>

		<el-dialog
			v-model="replyDialogVisible"
			title="回复留言"
			width="min(32rem, calc(100vw - 2rem))"
		>
			<el-form
				label-position="top"
				@submit.prevent
			>
				<el-form-item label="回复内容">
					<el-input
						v-model="replyContent"
						type="textarea"
						:rows="5"
						maxlength="1000"
						show-word-limit
						placeholder="请输入回复内容"
					/>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="replyDialogVisible = false">取消</el-button>
				<el-button
					type="primary"
					:loading="submitting"
					:disabled="!replyContent.trim() || submitting"
					@click="submitReply"
				>
					确认回复
				</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import type { TableInstance } from 'element-plus';
import { formatDateTime } from '@/utils/datetime';
import { useAdminMessageList } from '../composables/useAdminMessageList';
import {
	MESSAGE_MODERATION_ACTION,
	MESSAGE_STATUS,
	MESSAGE_TYPE,
	type AdminMessageItem,
	type MessageStatus,
} from '../types/adminMessage';
import { useMediaQuery } from '@vueuse/core';

const isMobile = useMediaQuery('(width < 768px)');

defineOptions({ name: 'AdminMessageListPage' });

const {
	messageList,
	selectedMessageIds,
	submitting,
	pageMeta,
	pageParams,
	filterForm,
	getMessageList,
	resetFilterForm,
	handleModerate,
	reply,
	approveSelected,
	canApprove,
	canReply,
	canReject,
	canHide,
	canDelete,
} = useAdminMessageList();

const tableRef = ref<TableInstance>();
const replyDialogVisible = ref(false);
const replyTarget = ref<AdminMessageItem | null>(null);
const replyContent = ref('');

onMounted(() => getMessageList());
watch(messageList, async () => {
	await nextTick();
	tableRef.value?.setScrollTop(0);
});

function handleSelectionChange(items: AdminMessageItem[]) {
	selectedMessageIds.value = items.map((item) => item.id);
}

function isBatchSelectable(item: AdminMessageItem) {
	return item.parentId === null && item.status === MESSAGE_STATUS.PENDING;
}

function openReplyDialog(item: AdminMessageItem) {
	replyTarget.value = item;
	replyContent.value = '';
	replyDialogVisible.value = true;
}

async function submitReply() {
	if (!replyTarget.value || !replyContent.value.trim()) return;
	const success = await reply(replyTarget.value, replyContent.value.trim());
	if (success) replyDialogVisible.value = false;
}

function getStatusLabel(status: MessageStatus) {
	return {
		[MESSAGE_STATUS.PENDING]: '待审核',
		[MESSAGE_STATUS.APPROVED]: '已通过',
		[MESSAGE_STATUS.REJECTED]: '已拒绝',
		[MESSAGE_STATUS.HIDDEN]: '已隐藏',
		[MESSAGE_STATUS.DELETED]: '已删除',
	}[status];
}

function getStatusTagType(status: MessageStatus) {
	if (status === MESSAGE_STATUS.APPROVED) return 'success';
	if (status === MESSAGE_STATUS.PENDING) return 'warning';
	if (status === MESSAGE_STATUS.REJECTED || status === MESSAGE_STATUS.DELETED) return 'danger';
	return 'info';
}

function getRowClassName({ row }: { row: AdminMessageItem }) {
	return row.status === MESSAGE_STATUS.REJECTED ||
		row.status === MESSAGE_STATUS.HIDDEN ||
		row.status === MESSAGE_STATUS.DELETED
		? 'message-row-muted'
		: '';
}
</script>

<style scoped lang="scss">
.admin-message {
	height: 100%;
	min-height: 0;
}

.admin-message__card {
	display: flex;
	height: 100%;
	flex-direction: column;
	border: none;
	background-color: var(--app-bg);
	box-shadow: none;
}

.admin-message__card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-message__filters {
	display: grid;
	grid-template-columns: repeat(4, minmax(0rem, 1fr));
	gap: 0 0.5rem;

	:deep(.el-input),
	:deep(.el-input-number) {
		width: 100%;
		min-width: 0;
	}

	.admin-message__filter-actions {
		margin-bottom: 1rem;
		display: flex;
		gap: 0.5rem;
	}

	// Element Plus 默认给相邻按钮添加了左边距，记得去掉，否则相邻两个按钮除了 grid 布局的 gap 之外还会有额外的间距
	:deep(.el-button) {
		margin: 0;
	}

	@media (width < 768px) {
		grid-template-columns: repeat(2, minmax(0rem, 1fr));

		.admin-message__filter-actions {
			grid-column: 1 / -1;

			.el-button {
				flex: 1;
			}
		}
	}

	@media (width < 376px) {
		grid-template-columns: repeat(1, minmax(0rem, 1fr));
	}
}

.admin-message__table-wrap {
	min-height: 0;
	flex: 1;
}

.admin-message__content {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	line-clamp: 2;
	white-space: pre-wrap;
}

.admin-message__secondary {
	margin: 0.15rem 0 0;
	color: var(--app-text-muted);
	font-size: 0.75rem;
}

.admin-message__notify-icon {
	display: flex;
	justify-content: center;
	align-items: center;
	font-size: 1.25rem;
}

:deep(.message-row-muted),
:deep(.message-row-muted td),
:deep(.message-row-muted .cell),
:deep(.message-row-muted p),
:deep(.message-row-muted strong) {
	color: var(--app-text-muted);
}

.admin-message__pagination {
	margin-top: 1.25rem;
}
</style>
