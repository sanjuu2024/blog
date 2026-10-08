<template>
	<el-card class="security-event-card">
		<template #header>
			<span class="text-xl font-bold">安全事件</span>
		</template>

		<el-form
			:model="filterForm"
			label-width="auto"
			@submit.prevent
		>
			<div class="security-event-filters">
				<el-form-item label="事件类型">
					<el-select
						v-model="filterForm.eventType"
						clearable
						placeholder="请选择事件类型"
					>
						<el-option
							v-for="item in eventTypeOptions"
							:key="item.value"
							:label="item.label"
							:value="item.value"
						/>
					</el-select>
				</el-form-item>
				<el-form-item label="事件结果">
					<el-select
						v-model="filterForm.outcome"
						clearable
						placeholder="请选择事件结果"
					>
						<el-option
							label="成功"
							:value="SECURITY_EVENT_OUTCOME.SUCCESS"
						/>
						<el-option
							label="失败"
							:value="SECURITY_EVENT_OUTCOME.FAILURE"
						/>
					</el-select>
				</el-form-item>
				<el-form-item label="用户 ID">
					<el-input-number
						v-model="filterForm.userId"
						:min="1"
						:controls="false"
						placeholder="请输入用户 ID"
					/>
				</el-form-item>
				<el-form-item label="事件时间">
					<el-date-picker
						v-model="filterForm.createdAtRange"
						type="datetimerange"
						range-separator="至"
						start-placeholder="开始时间"
						end-placeholder="结束时间"
						value-format="YYYY-MM-DDTHH:mm:ssZ"
					/>
				</el-form-item>
				<div class="security-event-actions">
					<el-button
						type="primary"
						@click="getSecurityEventList(1)"
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
		</el-form>

		<div class="security-event-table-wrap">
			<el-table
				v-loading="loading"
				:data="securityEventList"
				height="100%"
				row-key="id"
			>
				<el-table-column
					label="事件时间"
					width="180"
					align="center"
				>
					<template #default="{ row }: { row: SecurityEventItem }">
						{{ formatDateTime(row.createdAt) }}
					</template>
				</el-table-column>
				<el-table-column
					label="事件"
					min-width="150"
				>
					<template #default="{ row }: { row: SecurityEventItem }">
						<strong>{{ eventTypeLabel(row.eventType) }}</strong>
						<p class="security-event-secondary">{{ row.description }}</p>
					</template>
				</el-table-column>
				<el-table-column
					label="结果"
					width="90"
					align="center"
				>
					<template #default="{ row }: { row: SecurityEventItem }">
						<el-tag
							:type="
								row.outcome === SECURITY_EVENT_OUTCOME.SUCCESS
									? 'success'
									: 'danger'
							"
						>
							{{ row.outcome === SECURITY_EVENT_OUTCOME.SUCCESS ? '成功' : '失败' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column
					label="相关用户"
					min-width="230"
				>
					<template #default="{ row }: { row: SecurityEventItem }">
						<template v-if="row.userId !== null">
							<p :class="{ 'security-event-deleted': row.userDeleted }">
								目标用户：{{
									row.userDeleted
										? '账号已注销'
										: row.userNickname || row.userUsername || '用户资料不存在'
								}}
								<span v-if="row.userUsername">（{{ row.userUsername }}）</span>
							</p>
							<p class="security-event-secondary">ID：{{ row.userId }}</p>
						</template>
						<p v-else-if="row.account">
							{{
								row.eventType === SECURITY_EVENT_TYPE.EMAIL_VERIFICATION
									? '邮箱'
									: '账号'
							}}：{{ row.account }}
						</p>
						<p
							v-else
							class="security-event-secondary"
						>
							未关联用户
						</p>
						<template v-if="row.actorId !== null">
							<p :class="{ 'security-event-deleted': row.actorDeleted }">
								操作者：{{
									row.actorDeleted
										? '账号已注销'
										: row.actorNickname || row.actorUsername || '用户资料不存在'
								}}
								<span v-if="row.actorUsername">（{{ row.actorUsername }}）</span>
							</p>
							<p class="security-event-secondary">ID：{{ row.actorId }}</p>
						</template>
					</template>
				</el-table-column>
				<el-table-column
					label="来源信息"
					min-width="250"
				>
					<template #default="{ row }: { row: SecurityEventItem }">
						<p>IP: {{ row.ip || '未记录' }}</p>
						<el-tooltip
							v-if="row.userAgent"
							placement="top"
							:content="row.userAgent"
						>
							<p class="security-event-secondary">
								{{ row.browser }} / {{ row.operatingSystem }} / {{ row.device }}
							</p>
						</el-tooltip>
						<p
							v-else
							class="security-event-secondary"
						>
							客户端: 未记录
						</p>
					</template>
				</el-table-column>
				<el-table-column
					label="请求"
					min-width="230"
					show-overflow-tooltip
				>
					<template #default="{ row }: { row: SecurityEventItem }">
						{{ row.requestMethod || '未记录' }} {{ row.requestPath }}
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
			@current-change="getSecurityEventList"
			@size-change="getSecurityEventList(1)"
			class="security-event-pagination"
		/>
	</el-card>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useMediaQuery } from '@vueuse/core';
import { formatDateTime } from '@/utils/datetime';
import { useAdminSecurityEventList } from '../composables/useAdminSecurityEventList';
import {
	SECURITY_EVENT_OUTCOME,
	SECURITY_EVENT_TYPE,
	type SecurityEventItem,
	type SecurityEventType,
} from '../types/adminSecurityEvent';

defineOptions({ name: 'AdminSecurityEventPage' });

const isMobile = useMediaQuery('(width < 768px)');
const {
	securityEventList,
	loading,
	pageMeta,
	pageParams,
	filterForm,
	getSecurityEventList,
	resetFilterForm,
} = useAdminSecurityEventList();

const eventTypeOptions = Object.values(SECURITY_EVENT_TYPE).map((value) => ({
	value,
	label: eventTypeLabel(value),
}));

onMounted(() => getSecurityEventList());

function eventTypeLabel(eventType: SecurityEventType) {
	return {
		[SECURITY_EVENT_TYPE.REGISTER]: '注册',
		[SECURITY_EVENT_TYPE.LOGIN]: '登录',
		[SECURITY_EVENT_TYPE.LOGOUT]: '退出登录',
		[SECURITY_EVENT_TYPE.TOKEN_REFRESH]: '刷新登录态',
		[SECURITY_EVENT_TYPE.EMAIL_VERIFICATION]: '邮箱验证码',
		[SECURITY_EVENT_TYPE.PASSWORD_CHANGE]: '修改密码',
		[SECURITY_EVENT_TYPE.EMAIL_CHANGE]: '修改邮箱',
		[SECURITY_EVENT_TYPE.USER_DELETE]: '注销账号',
		[SECURITY_EVENT_TYPE.USER_STATUS_CHANGE]: '修改用户状态',
		[SECURITY_EVENT_TYPE.USER_ROLE_CHANGE]: '修改用户角色',
	}[eventType];
}
</script>

<style scoped lang="scss">
.security-event-card {
	display: flex;
	height: 100%;
	flex-direction: column;
	border: none;
	background-color: var(--app-bg);
	box-shadow: none;
}

.security-event-card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.security-event-filters {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 0.75rem;
}

.security-event-actions {
	display: flex;
	grid-column: 1 / -1;
	justify-content: flex-end;
	margin-bottom: 1rem;
}

.security-event-table-wrap {
	min-height: 0;
	flex: 1;
}

.security-event-secondary {
	color: var(--app-text-muted);
	font-size: 0.8rem;
}

.security-event-deleted {
	color: var(--app-text-muted-more);
}

.security-event-pagination {
	flex: none;
	margin-top: 1.5rem;
}

@media (width < 768px) {
	.security-event-filters {
		grid-template-columns: 1fr;
	}
}
</style>
