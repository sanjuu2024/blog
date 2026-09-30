<template>
	<div class="admin-mail-delivery">
		<el-card class="admin-mail-delivery__card">
			<template #header>
				<span class="text-xl font-bold">邮件投递</span>
			</template>
			<el-form
				:model="filters"
				label-width="auto"
				label-position="right"
				@submit.prevent="load(1)"
			>
				<div class="admin-mail-delivery__filter-row">
					<el-form-item
						prop="mailType"
						label="邮件类型"
					>
						<el-select
							v-model="filters.mailType"
							clearable
							placeholder="请选择邮件类型"
						>
							<el-option
								label="留言回复"
								value="MESSAGE_REPLY"
							/>
							<el-option
								label="评论回复"
								value="COMMENT_REPLY"
							/>
						</el-select>
					</el-form-item>
					<el-form-item
						prop="status"
						label="投递状态"
					>
						<el-select
							v-model="filters.status"
							clearable
							placeholder="请选择投递状态"
						>
							<el-option
								label="待发送"
								value="PENDING"
							/>
							<el-option
								label="已发送"
								value="SENT"
							/>
							<el-option
								label="失败"
								value="FAILED"
							/>
						</el-select>
					</el-form-item>
					<div class="admin-mail-delivery__filter-actions">
						<el-button
							type="primary"
							aria-label="搜索"
							@click="load(1)"
						>
							<i-lets-icons-search-alt />
							查询
						</el-button>
						<el-button
							aria-label="重置"
							@click="reset"
						>
							<i-lets-icons-refresh />
							重置
						</el-button>
					</div>
				</div>
			</el-form>
			<div class="admin-mail-delivery__table-wrap">
				<el-table
					:data="records"
					v-loading="loading"
					height="100%"
				>
					<el-table-column
						label="类型"
						align="center"
					>
						<template #default="{ row }">
							{{ getMailTypeLabel(row.mailType) }}
						</template>
					</el-table-column>
					<el-table-column
						prop="recipientMasked"
						label="收件地址"
						align="center"
					/>
					<el-table-column
						label="状态"
						align="center"
						width="100"
					>
						<template #default="{ row }">
							<el-tag
								effect="plain"
								:type="getStatusTagType(row.status)"
								class="font-bold"
							>
								{{ getStatusLabel(row.status) }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column
						prop="attemptCount"
						label="尝试次数"
						width="100"
						align="center"
					/>
					<el-table-column
						label="最近失败摘要"
						min-width="180"
						align="center"
					>
						<template #default="{ row }">
							<span class="admin-mail-delivery__error-summary">
								{{ getFailureSummary(row) }}
							</span>
						</template>
					</el-table-column>
					<el-table-column
						label="操作"
						width="180"
						align="center"
					>
						<template #default="{ row }">
							<div class="admin-mail-delivery__actions">
								<el-button
									type="primary"
									@click="showDetail(row.id)"
								>
									<i-lucide-send class="mr-1" />
									查看
								</el-button>
								<el-button
									v-if="row.status === 'FAILED'"
									type="warning"
									@click="retry(row.id)"
								>
									<i-lucide-refresh-cw class="mr-1" />
									重试
								</el-button>
							</div>
						</template>
					</el-table-column>
				</el-table>
			</div>
			<el-pagination
				v-model:current-page="pageNum"
				v-model:page-size="pageSize"
				:background="true"
				:layout="
					isMobile
						? 'prev, next, jumper, total'
						: 'prev, pager, next, jumper, ->, sizes, total'
				"
				:total="total"
				:pager-count="pagerCount"
				:page-sizes="[5, 10, 20, 50]"
				@current-change="load"
				@size-change="load(1)"
				class="admin-mail-delivery__pagination"
			/>
		</el-card>

		<el-dialog
			v-model="detailVisible"
			title="邮件投递详情"
			width="min(560px, calc(100vw - 2rem))"
		>
			<el-skeleton
				v-if="detailLoading"
				:rows="6"
				animated
			/>
			<el-descriptions
				v-else-if="detail"
				:border="true"
				:column="1"
			>
				<el-descriptions-item label="类型">
					{{ getMailTypeLabel(detail.mailType) }}
				</el-descriptions-item>
				<el-descriptions-item label="脱敏收件地址">
					{{ detail.recipientMasked }}
				</el-descriptions-item>
				<el-descriptions-item label="投递状态">
					<el-tag
						effect="plain"
						:type="getStatusTagType(detail.status)"
					>
						{{ getStatusLabel(detail.status) }}
					</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="尝试次数">
					{{ detail.attemptCount }}
				</el-descriptions-item>
				<el-descriptions-item label="最近失败类型">
					{{ detail.lastErrorType || '-' }}
				</el-descriptions-item>
				<el-descriptions-item label="最近失败摘要">
					{{ getFailureSummary(detail) }}
				</el-descriptions-item>
				<el-descriptions-item label="创建时间">
					{{ formatDateTime(detail.createdAt) }}
				</el-descriptions-item>
				<el-descriptions-item label="最近尝试时间">
					{{ formatDateTime(detail.lastAttemptAt) }}
				</el-descriptions-item>
				<el-descriptions-item label="发送成功时间">
					{{ formatDateTime(detail.sentAt) }}
				</el-descriptions-item>
			</el-descriptions>
			<el-empty
				v-else
				description="暂无投递详情"
			/>
		</el-dialog>
	</div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import { useMediaQuery } from '@vueuse/core';
import { formatDateTime } from '@/utils/datetime';
import {
	getMailDelivery,
	listMailDeliveries,
	retryMailDelivery,
} from '../api/adminMailDeliveryApi';
import type {
	AdminMailDeliveryItem,
	MailDeliveryStatus,
	MailType,
} from '../types/adminMailDelivery';

defineOptions({ name: 'AdminMailDeliveryPage' });
const records = ref<AdminMailDeliveryItem[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const isMobile = useMediaQuery('(width < 768px)');
const pagerCount = 5;
const filters = reactive<{ mailType?: MailType; status?: MailDeliveryStatus }>({});
const detail = ref<AdminMailDeliveryItem | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);

// 查询时同步服务端分页信息，保证筛选和翻页使用同一份状态。
async function load(page = pageNum.value) {
	pageNum.value = page;
	loading.value = true;
	try {
		const data = await listMailDeliveries({
			...filters,
			pageNum: page,
			pageSize: pageSize.value,
		});
		records.value = data.records;
		total.value = data.total;
		pageNum.value = data.pageNum;
		pageSize.value = data.pageSize;
	} finally {
		loading.value = false;
	}
}

// 清空筛选后重新查询第一页。
function reset() {
	filters.mailType = undefined;
	filters.status = undefined;
	void load(1);
}

// 获取邮件类型展示文案。
function getMailTypeLabel(mailType: MailType) {
	return mailType === 'MESSAGE_REPLY' ? '留言回复通知' : '评论回复通知';
}

// 获取邮件投递状态展示文案。
function getStatusLabel(status: MailDeliveryStatus) {
	return {
		PENDING: '待发送',
		SENT: '已发送',
		FAILED: '失败',
	}[status];
}

// 获取邮件投递状态标签类型。
function getStatusTagType(status: MailDeliveryStatus) {
	return {
		PENDING: 'warning',
		SENT: 'success',
		FAILED: 'danger',
	}[status] as 'warning' | 'success' | 'danger';
}

// 组合异常类型和安全失败摘要，便于定位失败原因且不暴露异常堆栈。
function getFailureSummary(mailDelivery: AdminMailDeliveryItem) {
	if (!mailDelivery.lastErrorMessage) return '-';
	return mailDelivery.lastErrorType
		? `${mailDelivery.lastErrorType}：${mailDelivery.lastErrorMessage}`
		: mailDelivery.lastErrorMessage;
}

// 打开邮件投递详情，只展示后端返回的脱敏投递信息。
async function showDetail(id: number) {
	detail.value = null;
	detailVisible.value = true;
	detailLoading.value = true;
	try {
		detail.value = await getMailDelivery(id);
	} finally {
		detailLoading.value = false;
	}
}

// 只有 FAILED 记录允许重试，确认后刷新当前页查看最新状态。
async function retry(id: number) {
	await ElMessageBox.confirm('确认重试这条失败邮件吗？', '手动重试');
	await retryMailDelivery(id);
	ElMessage.success('已提交重试');
	await load();
}

onMounted(() => void load(1));
</script>

<style scoped lang="scss">
.admin-mail-delivery {
	height: 100%;
	min-height: 0;
}

.admin-mail-delivery__card {
	display: flex;
	height: 100%;
	flex-direction: column;
	border: none;
	background-color: var(--app-bg);
	box-shadow: none;
}

.admin-mail-delivery__card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-mail-delivery__table-wrap {
	min-height: 0;
	flex: 1;
}

.admin-mail-delivery__filter-row {
	display: grid;
	grid-template-columns: repeat(2, minmax(12rem, 1fr)) auto;
	align-items: start;
	gap: 0.5rem;

	:deep(.el-select) {
		width: 100%;
	}

	:deep(.el-button) {
		margin-left: 0;
	}
}

.admin-mail-delivery__filter-actions {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	margin-bottom: 1.125rem;
}

.admin-mail-delivery__error-summary {
	display: block;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.admin-mail-delivery__pagination {
	margin-top: 1.25rem;
}

.admin-mail-delivery__actions {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 0.5rem;

	:deep(.el-button) {
		margin-left: 0;
	}
}

@media (width < 768px) {
	.admin-mail-delivery__filter-row {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}

	.admin-mail-delivery__filter-actions {
		grid-column: 1 / -1;
		justify-content: flex-end;
	}
}

@media (width < 400px) {
	.admin-mail-delivery__filter-row {
		grid-template-columns: minmax(0, 1fr);
	}

	.admin-mail-delivery__filter-actions {
		grid-column: auto;
		justify-content: stretch;

		.el-button {
			flex: 1;
		}
	}
}
</style>
