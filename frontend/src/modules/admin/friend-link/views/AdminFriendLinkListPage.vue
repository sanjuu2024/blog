<template>
	<div class="admin-friend-link">
		<el-card class="admin-friend-link__card">
			<template #header>
				<div class="admin-friend-link__header">
					<span class="text-xl font-bold">友链管理</span>
					<el-button
						type="primary"
						@click="openCreateDrawer"
					>
						<i-lucide-plus class="mr-1" />
						新增友链
					</el-button>
				</div>
			</template>

			<el-form
				:model="filterForm"
				label-width="auto"
				@submit.prevent
			>
				<div class="admin-friend-link__filters">
					<el-form-item
						class="admin-friend-link__filter-keyword"
						label="名称"
					>
						<el-input
							v-model.trim="filterForm.keyword"
							clearable
							placeholder="输入网站名称"
							@keyup.enter="getFriendLinkList(1)"
						/>
					</el-form-item>
					<el-form-item
						class="admin-friend-link__filter-section"
						label="分区"
					>
						<el-select
							v-model="filterForm.section"
							clearable
							placeholder="全部分区"
							@change="getFriendLinkList(1)"
						>
							<el-option
								v-for="section in FRIEND_LINK_SECTIONS"
								:key="section.value"
								:label="section.label"
								:value="section.value"
							/>
						</el-select>
					</el-form-item>
					<el-form-item
						class="admin-friend-link__filter-status"
						label="状态"
					>
						<el-radio-group
							v-model="filterForm.status"
							@change="getFriendLinkList(1)"
						>
							<el-radio value="">全部</el-radio>
							<el-radio :value="FRIEND_LINK_STATUS.ENABLED">启用</el-radio>
							<el-radio :value="FRIEND_LINK_STATUS.DISABLED">停用</el-radio>
						</el-radio-group>
					</el-form-item>
					<div class="admin-friend-link__filter-actions">
						<el-button
							type="primary"
							@click="getFriendLinkList(1)"
							>查询</el-button
						>
						<el-button @click="resetFilterForm">重置</el-button>
					</div>
				</div>
			</el-form>

			<div class="admin-friend-link__table-wrap">
				<el-table
					ref="tableRef"
					v-loading="loading"
					:data="friendLinks"
					row-key="id"
					height="100%"
				>
					<el-table-column
						prop="id"
						label="ID"
						width="90"
						align="center"
					/>
					<el-table-column
						prop="name"
						label="网站名称"
						min-width="150"
					/>
					<el-table-column
						label="网站地址"
						min-width="220"
					>
						<template #default="{ row }: { row: AdminFriendLinkItem }">
							<a
								:href="row.url"
								target="_blank"
								rel="noopener noreferrer"
								class="admin-friend-link__url"
							>
								{{ row.url }}
							</a>
						</template>
					</el-table-column>
					<el-table-column
						label="网站描述"
						min-width="180"
					>
						<template #default="{ row }: { row: AdminFriendLinkItem }">
							<span :title="row.description">{{ row.description || '-' }}</span>
						</template>
					</el-table-column>
					<el-table-column
						label="分区"
						width="120"
						align="center"
					>
						<template #default="{ row }: { row: AdminFriendLinkItem }">
							{{ getSectionLabel(row.section) }}
						</template>
					</el-table-column>
					<el-table-column
						prop="sortNo"
						label="排序值"
						width="100"
						align="center"
					/>
					<el-table-column
						label="状态"
						width="90"
						align="center"
					>
						<template #default="{ row }: { row: AdminFriendLinkItem }">
							<el-tag
								:type="
									row.status === FRIEND_LINK_STATUS.ENABLED ? 'success' : 'info'
								"
								effect="plain"
								class="font-bold"
							>
								{{ row.status === FRIEND_LINK_STATUS.ENABLED ? '启用' : '停用' }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column
						label="创建时间"
						width="180"
						align="center"
					>
						<template #default="{ row }: { row: AdminFriendLinkItem }">
							{{ formatDateTime(row.createdAt) }}
						</template>
					</el-table-column>
					<el-table-column
						label="操作"
						width="230"
						align="center"
						:fixed="isMobile ? false : 'right'"
					>
						<template #default="{ row }: { row: AdminFriendLinkItem }">
							<div class="admin-friend-link__actions">
								<el-button
									type="warning"
									@click="openEditDrawer(row)"
									>编辑</el-button
								>
								<el-popconfirm
									:title="
										'确认' +
										(row.status === FRIEND_LINK_STATUS.ENABLED
											? '停用'
											: '启用') +
										'友链“' +
										row.name +
										'”吗？'
									"
									@confirm="toggleFriendLinkStatus(row)"
								>
									<template #reference>
										<el-button
											:type="
												row.status === FRIEND_LINK_STATUS.ENABLED
													? 'warning'
													: 'success'
											"
										>
											{{
												row.status === FRIEND_LINK_STATUS.ENABLED
													? '停用'
													: '启用'
											}}
										</el-button>
									</template>
								</el-popconfirm>
								<el-popconfirm
									:title="'确认删除友链“' + row.name + '”吗？'"
									@confirm="handleDeleteFriendLink(row)"
								>
									<template #reference>
										<el-button type="danger">删除</el-button>
									</template>
								</el-popconfirm>
							</div>
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
				@current-change="getFriendLinkList"
				@size-change="getFriendLinkList(1)"
			/>
		</el-card>

		<el-drawer
			v-model="drawerVisible"
			resizable
			direction="rtl"
			size="min(32rem, 100vw)"
		>
			<template #header>
				<p class="font-bold">{{ drawerMode === 'create' ? '新增友链' : '编辑友链' }}</p>
			</template>
			<el-form
				:model="form"
				:rules="rules"
				label-width="auto"
				:ref="setFormRef"
			>
				<el-form-item
					prop="name"
					label="网站名称"
				>
					<el-input
						v-model.trim="form.name"
						maxlength="100"
						show-word-limit
					/>
				</el-form-item>
				<el-form-item
					prop="url"
					label="网站地址"
				>
					<el-input
						v-model.trim="form.url"
						maxlength="500"
						placeholder="https://example.com"
					/>
				</el-form-item>
				<el-form-item
					prop="description"
					label="网站描述"
				>
					<el-input
						v-model.trim="form.description"
						type="textarea"
						maxlength="255"
						show-word-limit
					/>
				</el-form-item>
				<el-form-item
					prop="logoUrl"
					label="头像图片或网站代表图地址（可选）"
				>
					<el-input
						v-model.trim="form.logoUrl"
						maxlength="500"
						placeholder="https://example.com/avatar.png"
					/>
				</el-form-item>
				<el-form-item
					prop="section"
					label="分区"
				>
					<el-select v-model="form.section">
						<el-option
							v-for="section in FRIEND_LINK_SECTIONS"
							:key="section.value"
							:label="section.label"
							:value="section.value"
						/>
					</el-select>
				</el-form-item>
				<el-form-item
					prop="sortNo"
					label="排序值"
				>
					<el-input-number
						v-model="form.sortNo"
						:min="0"
						:precision="0"
						:step="100"
					/>
					<span class="admin-friend-link__help">数值越小越靠前，新建默认 100</span>
				</el-form-item>
				<el-form-item
					prop="status"
					label="状态"
				>
					<el-select v-model="form.status">
						<el-option
							label="启用"
							:value="FRIEND_LINK_STATUS.ENABLED"
						/>
						<el-option
							label="停用"
							:value="FRIEND_LINK_STATUS.DISABLED"
						/>
					</el-select>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="drawerVisible = false">取消</el-button>
				<el-button
					type="primary"
					:loading="submitting"
					@click="submit"
					>保存</el-button
				>
			</template>
		</el-drawer>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue';
import type { TableInstance } from 'element-plus';
import { useMediaQuery } from '@vueuse/core';
import { formatDateTime } from '@/utils/datetime';
import {
	FRIEND_LINK_SECTIONS,
	FRIEND_LINK_STATUS,
	type FriendLinkSection,
} from '@/modules/link/types/friendLink';
import { type AdminFriendLinkItem } from '../types/adminFriendLink';
import { useAdminFriendLinkList } from '../composables/useAdminFriendLinkList';
import { useAdminFriendLinkForm } from '../composables/useAdminFriendLinkForm';

defineOptions({ name: 'AdminFriendLinkListPage' });

const isMobile = useMediaQuery('(width < 768px)');
const tableRef = ref<TableInstance | null>(null);
const {
	friendLinks,
	loading,
	pageMeta,
	pageParams,
	filterForm,
	getFriendLinkList,
	resetFilterForm,
	handleDeleteFriendLink,
	toggleFriendLinkStatus,
} = useAdminFriendLinkList();
const {
	drawerVisible,
	drawerMode,
	submitting,
	form,
	rules,
	setFormRef,
	openCreateDrawer,
	openEditDrawer,
	submitForm,
} = useAdminFriendLinkForm();

onMounted(() => getFriendLinkList(1));

watch(friendLinks, async () => {
	await nextTick();
	if (typeof tableRef.value?.setScrollTop === 'function') {
		tableRef.value.setScrollTop(0);
	}
});

function getSectionLabel(section: FriendLinkSection) {
	return FRIEND_LINK_SECTIONS.find((item) => item.value === section)?.label ?? section;
}

async function submit() {
	if (await submitForm()) await getFriendLinkList(1);
}
</script>

<style scoped lang="scss">
.admin-friend-link {
	height: 100%;
	min-height: 0;
}

.admin-friend-link__card {
	display: flex;
	height: 100%;
	flex-direction: column;
	border: none;
	background-color: var(--app-bg);
	box-shadow: none;
}

.admin-friend-link__card :deep(.el-card__body) {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
}

.admin-friend-link__header,
.admin-friend-link__filter-actions,
.admin-friend-link__actions {
	display: flex;
	align-items: center;
	gap: 0.5rem;
}

.admin-friend-link__header {
	justify-content: space-between;
}

.admin-friend-link__filters {
	display: grid;
	grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
	grid-template-areas:
		'keyword section'
		'status actions';
	align-items: start;
	gap: 0 0.75rem;

	:deep(.el-input),
	:deep(.el-select) {
		width: 100%;
	}

	:deep(.el-form-item) {
		min-width: 0;
	}
}

.admin-friend-link__filter-keyword {
	grid-area: keyword;
}

.admin-friend-link__filter-section {
	grid-area: section;
}

.admin-friend-link__filter-status {
	grid-area: status;
}

.admin-friend-link__filter-actions {
	grid-area: actions;
	align-self: start;
	justify-content: flex-end;
	margin-bottom: 1.125rem;
}

.admin-friend-link__table-wrap {
	min-height: 0;
	flex: 1;
	margin-bottom: 1rem;
	overflow: auto;
}

.admin-friend-link__url {
	color: var(--app-main);
	word-break: break-all;
}

.admin-friend-link__help {
	width: 100%;
	color: var(--app-text-muted);
	font-size: 0.875rem;
}

@media (width < 580px) {
	.admin-friend-link__filters {
		column-gap: 0.5rem;
		grid-template-columns: minmax(0, 1fr);
		grid-template-areas:
			'keyword'
			'section'
			'status'
			'actions';
	}

	.admin-friend-link__filter-actions {
		grid-column: auto;
		justify-content: stretch;
	}
}
</style>
