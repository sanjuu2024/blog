<template>
	<el-dialog
		:model-value="modelValue"
		title="通知"
		width="min(42rem, calc(100vw - 2rem))"
		class="notification-dialog"
		@update:model-value="emit('update:modelValue', $event)"
	>
		<div class="notification-dialog__toolbar">
			<el-radio-group
				v-model="category"
				@change="handleCategoryChange"
			>
				<el-radio-button value="ALL">全部</el-radio-button>
				<el-radio-button value="REPLY">回复</el-radio-button>
				<el-radio-button value="ADMIN_MESSAGE">管理员消息</el-radio-button>
			</el-radio-group>
			<el-button
				:disabled="!hasUnreadInCategory || loading"
				@click="markAllRead"
			>
				全部已读
			</el-button>
		</div>

		<div
			v-loading="loading"
			class="notification-dialog__list"
		>
			<el-empty
				v-if="!notifications.length && !loading"
				description="暂无通知"
			/>
			<div
				v-for="notification in notifications"
				:key="notification.id"
				class="notification-item"
			>
				<button
					class="notification-item__toggle"
					:aria-expanded="expandedIds.has(notification.id)"
					@click="toggleNotification(notification)"
				>
					<i-lucide-chevron-right
						class="notification-item__chevron"
						:class="{
							'notification-item__chevron--expanded': expandedIds.has(
								notification.id,
							),
						}"
					/>
					<span
						v-if="!notification.read"
						class="notification-item__unread"
					/>
					<strong>{{ notification.title }}</strong>
					<time>{{ formatDateTime(notification.createdAt) }}</time>
				</button>
				<p
					v-if="expandedIds.has(notification.id)"
					class="notification-item__content"
				>
					{{ notification.content }}
				</p>
			</div>
			<el-button
				v-if="page.hasNext"
				:loading="loading"
				class="notification-dialog__more"
				@click="loadNotifications(page.pageNum + 1)"
			>
				加载更多
			</el-button>
		</div>
	</el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { formatDateTime } from '@/utils/datetime';
import { useNotifications } from '../composables/useNotifications';
import type { NotificationItem } from '../types/notification';

defineOptions({ name: 'NotificationDialog' });

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();

const {
	notifications,
	page,
	category,
	hasUnreadInCategory,
	loading,
	loadUnreadCount,
	loadNotifications,
	changeCategory,
	markRead,
	markAllRead,
} = useNotifications();
const expandedIds = ref(new Set<number>());

watch(
	() => props.modelValue,
	(open) => {
		if (!open) return;
		expandedIds.value = new Set();
		loadNotifications();
		loadUnreadCount();
	},
	{ immediate: true },
);

function handleCategoryChange(value: string | number | boolean | undefined) {
	if (value === 'ALL' || value === 'REPLY' || value === 'ADMIN_MESSAGE') {
		changeCategory(value);
	}
}

async function toggleNotification(notification: NotificationItem) {
	const next = new Set(expandedIds.value);
	if (next.has(notification.id)) {
		next.delete(notification.id);
	} else {
		next.add(notification.id);
		expandedIds.value = next;
		try {
			await markRead(notification);
		} catch {
			// 请求错误由统一响应拦截器提示。
		}
		return;
	}
	expandedIds.value = next;
}
</script>

<style scoped lang="scss">
.notification-dialog__toolbar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 0.75rem;
	margin-bottom: 0.75rem;
	flex-wrap: wrap;
}

.notification-dialog__list {
	min-height: 8rem;
	max-height: 60dvh;
	overflow-y: auto;
}

.notification-item {
	border-bottom: 1px solid var(--app-border);
}

.notification-item__toggle {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	width: 100%;
	min-height: 3.5rem;
	padding: 0.75rem 0.25rem;
	border: 0;
	background: transparent;
	color: var(--app-text);
	text-align: left;
	cursor: pointer;
}

.notification-item__chevron {
	width: 1rem;
	height: 1rem;
	flex: none;
	transition: transform 0.1s ease-in-out;
}

.notification-item__chevron--expanded {
	transform: rotate(90deg);
}

.notification-item__unread {
	width: 0.45rem;
	height: 0.45rem;
	flex: none;
	border-radius: 50%;
	background: var(--el-color-primary);
}

.notification-item__toggle strong {
	min-width: 0;
	flex: 1;
	font-weight: 600;
}

.notification-item__toggle time {
	flex: none;
	color: var(--app-text-muted);
	font-size: 0.75rem;
}

.notification-item__content {
	margin: 0 0 0.75rem 1.5rem;
	color: var(--app-text-muted);
	white-space: pre-wrap;
	overflow-wrap: anywhere;
}

.notification-dialog__more {
	display: block;
	margin: 1rem auto 0;
}

@media (width < 480px) {
	.notification-item__toggle {
		flex-wrap: wrap;
	}

	.notification-item__toggle time {
		width: 100%;
		margin-left: 1.5rem;
	}
}
</style>
