<template>
	<section class="admin-notification">
		<header class="admin-notification__header">
			<h1>管理员消息</h1>
			<el-button
				type="primary"
				@click="openCreate"
			>
				<i-lucide-plus class="mr-1" />
				新建消息
			</el-button>
		</header>

		<el-table
			:data="notifications"
			v-loading="loading"
			class="admin-notification__table"
		>
			<el-table-column
				prop="title"
				label="标题"
				min-width="180"
			/>
			<el-table-column
				label="目标"
				width="140"
				align="center"
			>
				<template #default="{ row }">
					{{ row.targetScope === 'ALL_USERS' ? '全部用户' : `指定用户 ${row.userId}` }}
				</template>
			</el-table-column>
			<el-table-column
				label="状态"
				width="110"
				align="center"
			>
				<template #default="{ row }">{{ statusLabel(row.status) }}</template>
			</el-table-column>
			<el-table-column
				prop="createdAt"
				label="创建时间"
				width="180"
				align="center"
			>
				<template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
			</el-table-column>
			<el-table-column
				label="操作"
				width="150"
				align="center"
			>
				<template #default="{ row }">
					<div class="admin-notification__actions flex items-center justify-center gap-2">
						<el-button
							v-if="row.status === 'DRAFT'"
							type="primary"
							@click="openEdit(row)"
						>
							编辑
						</el-button>

						<el-popconfirm
							:title="`确认发布消息 ${row.title} 吗？`"
							v-if="row.status === 'DRAFT' || row.status === 'OFFLINE'"
							@confirm="changeStatus(row.id, 'PUBLISHED')"
						>
							<template #reference>
								<el-button type="success"> 发布 </el-button>
							</template>
						</el-popconfirm>

						<el-popconfirm
							:title="`确认下线消息 ${row.title} 吗？`"
							v-if="row.status === 'PUBLISHED'"
							@confirm="changeStatus(row.id, 'OFFLINE')"
						>
							<template #reference>
								<el-button type="warning"> 下线 </el-button>
							</template>
						</el-popconfirm>
					</div>
				</template>
			</el-table-column>
		</el-table>

		<el-dialog
			v-model="dialogVisible"
			:title="editingId === null ? '新建管理员消息' : '编辑消息草稿'"
			width="min(36rem, calc(100vw - 2rem))"
		>
			<el-form label-position="top">
				<el-form-item label="标题">
					<el-input
						v-model.trim="form.title"
						maxlength="100"
					/>
				</el-form-item>
				<el-form-item label="目标范围">
					<el-radio-group v-model="form.targetScope">
						<el-radio value="ALL_USERS">全部用户</el-radio>
						<el-radio value="SELECTED_USERS">指定用户</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item
					label="用户 ID"
					v-if="form.targetScope === 'SELECTED_USERS'"
				>
					<el-input
						v-model="userIdText"
						placeholder="请输入用户 ID"
					/>
				</el-form-item>
				<el-form-item label="内容">
					<el-input
						v-model.trim="form.content"
						type="textarea"
						:rows="6"
						maxlength="2000"
						show-word-limit
					/>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialogVisible = false">取消</el-button>
				<el-button
					:loading="submitting"
					@click="submit('DRAFT')"
				>
					保存草稿
				</el-button>
				<el-button
					type="primary"
					:loading="submitting"
					@click="submit('PUBLISHED')"
				>
					{{ editingId === null ? '发送' : '保存并发布' }}
				</el-button>
			</template>
		</el-dialog>
	</section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { formatDateTime } from '@/utils/datetime';
import {
	createAdminNotification,
	listAdminNotifications,
	updateAdminNotification,
	updateAdminNotificationStatus,
} from '../api/adminNotificationApi';
import type {
	AdminNotificationItem,
	CreateAdminNotificationRequest,
	NotificationStatus,
} from '../types/adminNotification';

defineOptions({ name: 'AdminNotificationPage' });

const notifications = ref<AdminNotificationItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const userIdText = ref('');
const form = reactive<CreateAdminNotificationRequest>({
	targetScope: 'ALL_USERS',
	title: '',
	content: '',
});

function statusLabel(status: NotificationStatus) {
	return { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }[status];
}

async function load() {
	loading.value = true;
	try {
		notifications.value = await listAdminNotifications();
	} finally {
		loading.value = false;
	}
}

function openCreate() {
	editingId.value = null;
	form.targetScope = 'ALL_USERS';
	form.title = '';
	form.content = '';
	userIdText.value = '';
	dialogVisible.value = true;
}

function openEdit(item: AdminNotificationItem) {
	editingId.value = item.id;
	form.targetScope = item.targetScope;
	form.title = item.title;
	form.content = item.content;
	userIdText.value = item.userId === null ? '' : String(item.userId);
	dialogVisible.value = true;
}

async function submit(status: 'DRAFT' | 'PUBLISHED') {
	const rawUserId = userIdText.value.trim();
	if (
		form.targetScope === 'SELECTED_USERS' &&
		(!/^[1-9]\d*$/.test(rawUserId) || !Number.isSafeInteger(Number(rawUserId)))
	) {
		ElMessage.warning('请填写有效的用户 ID');
		return;
	}
	const data: CreateAdminNotificationRequest = {
		...form,
		status,
		userId: form.targetScope === 'SELECTED_USERS' ? Number(rawUserId) : undefined,
	};
	if (!data.title || !data.content) {
		ElMessage.warning('请填写标题和内容');
		return;
	}
	submitting.value = true;
	try {
		if (editingId.value === null) {
			await createAdminNotification(data);
		} else {
			await updateAdminNotification(editingId.value, data);
			if (status === 'PUBLISHED') {
				await updateAdminNotificationStatus(editingId.value, 'PUBLISHED');
			}
		}
		ElMessage.success(status === 'DRAFT' ? '草稿已保存' : '管理员消息已发送');
		dialogVisible.value = false;
		await load();
	} finally {
		submitting.value = false;
	}
}

async function changeStatus(id: number, status: NotificationStatus) {
	await updateAdminNotificationStatus(id, status);
	await load();
}

onMounted(load);
</script>

<style scoped lang="scss">
.admin-notification {
	height: 100%;
	min-height: 0;
	overflow: auto;
	padding: 1rem;
}

.admin-notification__header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 1rem;
}

.admin-notification__header h1 {
	margin: 0;
	font-size: 1.5rem;
	font-weight: 700;
}

.admin-notification__table {
	width: 100%;
}

.admin-notification__actions {
	:deep(.el-button) {
		margin-left: 0;
	}
}
</style>
