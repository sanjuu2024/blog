<template>
	<div class="admin-audit-log">
		<el-card class="admin-audit-log__card">
			<template #header>
				<span class="text-xl font-bold">操作审计</span>
			</template>

			<el-form
				:model="filterForm"
				label-width="auto"
				label-position="right"
				@submit.prevent
			>
				<div class="admin-audit-log__filters">
					<div class="admin-audit-log__filter-row admin-audit-log__filter-row--primary">
						<el-form-item
							prop="operatorId"
							label="操作者 ID"
						>
							<el-input-number
								v-model="filterForm.operatorId"
								:min="1"
								:controls="false"
								placeholder="请输入操作者 ID"
							/>
						</el-form-item>
						<el-form-item
							prop="resourceType"
							label="资源类型"
						>
							<el-select
								v-model="filterForm.resourceType"
								clearable
								placeholder="请选择资源类型"
							>
								<el-option
									v-for="item in resourceTypeOptions"
									:key="item.value"
									:label="item.label"
									:value="item.value"
								/>
							</el-select>
						</el-form-item>
						<el-form-item
							prop="resourceId"
							label="目标资源"
						>
							<el-input
								v-model="filterForm.resourceId"
								clearable
								maxlength="255"
								placeholder="请输入目标资源标识"
							/>
						</el-form-item>
						<el-form-item
							prop="action"
							label="操作类型"
						>
							<el-select
								v-model="filterForm.action"
								clearable
								placeholder="请选择操作类型"
							>
								<el-option
									v-for="item in actionOptions"
									:key="item.value"
									:label="item.label"
									:value="item.value"
								/>
							</el-select>
						</el-form-item>
						<el-form-item
							prop="result"
							label="操作结果"
						>
							<el-select
								v-model="filterForm.result"
								clearable
								placeholder="请选择操作结果"
							>
								<el-option
									label="成功"
									:value="AUDIT_RESULT.SUCCESS"
								/>
								<el-option
									label="失败"
									:value="AUDIT_RESULT.FAILURE"
								/>
							</el-select>
						</el-form-item>
					</div>

					<div class="admin-audit-log__filter-row admin-audit-log__filter-row--secondary">
						<el-form-item
							prop="createdAtRange"
							label="操作时间"
							class="admin-audit-log__op-time-form-item"
						>
							<el-date-picker
								v-model="filterForm.createdAtRange"
								type="datetimerange"
								range-separator="至"
								start-placeholder="开始时间"
								end-placeholder="结束时间"
								value-format="YYYY-MM-DDTHH:mm:ssZ"
							/>
						</el-form-item>
						<div class="admin-audit-log__filter-actions">
							<el-button
								type="primary"
								@click="getAuditLogList(1)"
							>
								<i-lets-icons-search-alt />
								查询
							</el-button>
							<el-button @click="resetFilterForm">
								<i-lets-icons-refresh />
								重置
							</el-button>
						</div>
					</div>
				</div>
			</el-form>

			<div class="admin-audit-log__table-wrap">
				<el-table
					v-loading="loading"
					:data="auditLogList"
					height="100%"
					row-key="id"
				>
					<el-table-column
						prop="id"
						label="ID"
						align="center"
						width="90"
					/>
					<el-table-column
						label="操作者"
						min-width="150"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							<strong>{{ row.operatorUsername }}</strong>
							<p class="admin-audit-log__secondary">用户 ID: {{ row.operatorId }}</p>
						</template>
					</el-table-column>
					<el-table-column
						label="目标资源"
						align="center"
						min-width="190"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							<div class="admin-audit-log__resource">
								<el-tag
									type="info"
									class="font-bold"
									>{{ getResourceTypeLabel(row.resourceType) }}</el-tag
								>
								<el-tooltip
									v-if="row.resourceId"
									:content="row.resourceId"
									placement="bottom"
									effect="light"
								>
									<span class="admin-audit-log__resource-id">{{
										row.resourceId
									}}</span>
								</el-tooltip>
								<p
									v-else
									class="admin-audit-log__secondary"
								>
									未生成资源 ID
								</p>
							</div>
						</template>
					</el-table-column>
					<el-table-column
						label="操作"
						min-width="150"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							<strong>{{ getActionLabel(row.action) }}</strong>
							<p
								v-if="row.actionDetail"
								class="admin-audit-log__secondary"
							>
								{{ getDetailLabel(row.actionDetail) }}
							</p>
						</template>
					</el-table-column>
					<el-table-column
						label="结果"
						align="center"
						width="90"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							<el-tag
								effect="plain"
								:type="row.result === AUDIT_RESULT.SUCCESS ? 'success' : 'danger'"
								class="admin-audit-log__result-tag font-bold"
							>
								{{ row.result === AUDIT_RESULT.SUCCESS ? '成功' : '失败' }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column
						label="失败原因"
						min-width="190"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							<template v-if="row.result === AUDIT_RESULT.FAILURE">
								<p>{{ row.failureMessage || '操作失败' }}</p>
								<p class="admin-audit-log__secondary">
									业务码: {{ row.failureCode }}
								</p>
							</template>
							<span v-else>-</span>
						</template>
					</el-table-column>
					<el-table-column
						label="请求"
						min-width="250"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							<el-tag
								effect="plain"
								:class="getRequestMethodClass(row.requestMethod)"
							>
								{{ row.requestMethod }}
							</el-tag>
							<p
								:title="row.requestPath"
								class="admin-audit-log__ellipsis"
							>
								{{ row.requestPath }}
							</p>
						</template>
					</el-table-column>
					<el-table-column
						label="操作时间"
						align="center"
						width="180"
						:fixed="isMobile ? false : 'right'"
					>
						<template #default="{ row }: { row: AdminAuditLogItem }">
							{{ formatDateTime(row.createdAt) }}
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
				:page-sizes="[10, 20, 50, 100]"
				@current-change="getAuditLogList"
				@size-change="getAuditLogList(1)"
				class="admin-audit-log__pagination"
			/>
		</el-card>
	</div>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core';
import { ElMessage } from 'element-plus';
import { onMounted } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import { useAdminAuditLogList } from '../composables/useAdminAuditLogList';
import {
	AUDIT_ACTION,
	AUDIT_RESOURCE_TYPE,
	AUDIT_RESULT,
	type AdminAuditLogItem,
	type AuditAction,
	type AuditResourceType,
} from '../types/adminAudit';
import { useMediaQuery } from '@vueuse/core';

const isMobile = useMediaQuery('(width < 768px)');

defineOptions({ name: 'AdminAuditLogListPage' });

const {
	auditLogList,
	loading,
	pageMeta,
	pageParams,
	filterForm,
	getAuditLogList,
	resetFilterForm,
} = useAdminAuditLogList();

const resourceTypeOptions = Object.values(AUDIT_RESOURCE_TYPE).map((value) => ({
	value,
	label: getResourceTypeLabel(value),
}));
const actionOptions = Object.values(AUDIT_ACTION).map((value) => ({
	value,
	label: getActionLabel(value),
}));
const { copy } = useClipboard({ legacy: true });

onMounted(() => getAuditLogList());

// 资源标识可能是 ID、批量 ID 或图片 URL，点击后统一复制原始完整值。
async function copyResourceId(resourceId: string) {
	try {
		await copy(resourceId);
		ElMessage.success('目标资源标识已复制');
	} catch {
		ElMessage.error('复制失败，请手动复制');
	}
}

function getResourceTypeLabel(resourceType: AuditResourceType) {
	return {
		[AUDIT_RESOURCE_TYPE.USER]: '用户',
		[AUDIT_RESOURCE_TYPE.ARTICLE]: '文章',
		[AUDIT_RESOURCE_TYPE.CATEGORY]: '分类',
		[AUDIT_RESOURCE_TYPE.TAG]: '标签',
		[AUDIT_RESOURCE_TYPE.COMMENT]: '评论',
		[AUDIT_RESOURCE_TYPE.MESSAGE]: '留言',
		[AUDIT_RESOURCE_TYPE.FILE]: '文件',
	}[resourceType];
}

function getActionLabel(action: AuditAction) {
	return {
		[AUDIT_ACTION.CREATE]: '创建',
		[AUDIT_ACTION.UPDATE]: '更新',
		[AUDIT_ACTION.DELETE]: '删除',
		[AUDIT_ACTION.CHANGE_STATUS]: '修改状态',
		[AUDIT_ACTION.CHANGE_ROLE]: '修改角色',
		[AUDIT_ACTION.MODERATE]: '审核处理',
		[AUDIT_ACTION.REPLY]: '回复',
		[AUDIT_ACTION.BATCH_APPROVE]: '批量通过',
		[AUDIT_ACTION.UPLOAD]: '上传',
	}[action];
}

function getDetailLabel(detail: string) {
	const labels: Record<string, string> = {
		APPROVE: '通过',
		REJECT: '拒绝',
		HIDE: '隐藏',
		DELETE: '删除',
		ACTIVE: '启用',
		DISABLED: '禁用',
		ADMIN: '管理员',
		USER: '普通用户',
		DRAFT: '草稿',
		PUBLISHED: '已发布',
		OFFLINE: '已下线',
		ENABLED: '启用',
		ARTICLE_COVER: '文章封面',
		ARTICLE_CONTENT: '文章正文',
		PROJECT_COVER: '项目封面',
		'1': '一级分类',
		'2': '二级分类',
	};
	return labels[detail] || detail;
}

// HTTP 方法使用与 API 调试工具一致的语义色，未知方法回退为中性灰色。
function getRequestMethodClass(method: string) {
	const methodClass: Record<string, string> = {
		GET: 'admin-audit-log__method--get',
		POST: 'admin-audit-log__method--post',
		PUT: 'admin-audit-log__method--put',
		PATCH: 'admin-audit-log__method--patch',
		DELETE: 'admin-audit-log__method--delete',
		HEAD: 'admin-audit-log__method--head',
		OPTIONS: 'admin-audit-log__method--options', // 兜底
	};
	return ['admin-audit-log__method', methodClass[method.toUpperCase()]].filter(Boolean);
}
</script>

<style scoped lang="scss">
.admin-audit-log {
	height: 100%;
	min-height: 0;
}

.admin-audit-log__card {
	display: flex;
	height: 100%;
	flex-direction: column;
	border: none;
	background-color: var(--app-bg);
	box-shadow: none;
}

.admin-audit-log__card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-audit-log__filters {
	display: flex;
	flex-direction: column;
	gap: 0.5rem;
	margin-bottom: 1rem;
}

.admin-audit-log__filter-row {
	display: flex;
	align-items: center;
	gap: 0.5rem;
}

.admin-audit-log__filter-row--primary :deep(.el-input-number),
.admin-audit-log__filter-row--primary :deep(.el-input),
.admin-audit-log__filter-row--primary :deep(.el-select) {
	width: 100%;
	min-width: 0;
}

.admin-audit-log__filter-row--secondary {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.admin-audit-log__filter-actions {
	display: flex;
	align-items: center;
}

.admin-audit-log__table-wrap {
	min-height: 0;
	flex: 1;
}

.admin-audit-log__secondary {
	margin: 0.15rem 0 0;
	color: var(--app-text-muted);
	font-size: 0.8rem;
}

.admin-audit-log__resource {
	display: flex;
	min-width: 0;
	flex-direction: column;
	align-items: center;
}

.admin-audit-log__resource-id {
	display: inline-block;
	width: 100%;
	overflow: hidden;
	margin-top: 0.25rem;
	color: inherit;
	cursor: pointer;
	text-align: center;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.admin-audit-log__ellipsis {
	width: 100%;
	overflow: hidden;
	margin-top: 0.25rem;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.admin-audit-log__op-time-form-item {
	margin-bottom: 0;
}

.admin-audit-log__method {
	--audit-method-color: var(--app-text-muted);
	--el-tag-bg-color: color-mix(in srgb, var(--audit-method-color) 12%, transparent);
	--el-tag-border-color: color-mix(in srgb, var(--audit-method-color) 55%, transparent);
	--el-tag-text-color: var(--audit-method-color);
	font-weight: bold;
}

.admin-audit-log__method--get {
	--audit-method-color: #16a05d;
}

.admin-audit-log__method--post {
	--audit-method-color: #2878d0;
}

.admin-audit-log__method--put {
	--audit-method-color: #b98500;
}

.admin-audit-log__method--patch {
	--audit-method-color: #dc6814;
}

.admin-audit-log__method--delete {
	--audit-method-color: #d64545;
}

.admin-audit-log__method--head {
	--audit-method-color: #8056c8;
}

.admin-audit-log__method--options {
	--audit-method-color: var(--app-text-muted);
}

.admin-audit-log__pagination {
	margin-top: 1.25rem;
}

.admin-audit-log__filter-row--primary {
	display: grid;
	grid-template-columns: repeat(3, minmax(10rem, 1fr));
}

@media (width < 900px) {
	.admin-audit-log__filter-row--primary {
		grid-template-columns: repeat(2, minmax(10rem, 1fr));
	}

	.admin-audit-log__filter-row--secondary {
		align-items: stretch;
		flex-direction: column;
	}

	.admin-audit-log__filter-actions {
		justify-content: flex-end;
	}
}
</style>
