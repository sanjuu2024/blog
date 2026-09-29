<template>
	<el-dialog
		:model-value="modelValue"
		width="min(42rem, calc(100vw - 2rem))"
		class="notification-dialog"
		@update:model-value="emit('update:modelValue', $event)"
	>
		<template #header>
			<div class="notif-dialog__header flex items-center gap-2">
				<i-lucide-bell />
				<span>通知</span>
			</div>
		</template>
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
				:class="{ 'notification-item--expanded': expandedIds.has(notification.id) }"
				@click="openSource(notification)"
			>
				<div class="notification-item__header">
					<button
						class="notification-item__toggle"
						:aria-label="expandedIds.has(notification.id) ? '收起通知' : '展开通知'"
						:aria-expanded="expandedIds.has(notification.id)"
						@click.stop="toggleNotification(notification)"
					>
						<i-lucide-chevron-right
							class="notification-item__chevron"
							:class="{
								'notification-item__chevron--expanded': expandedIds.has(
									notification.id,
								),
							}"
						/>
					</button>
					<span
						v-if="!notification.read"
						class="notification-item__unread"
					/>
					<button class="notification-item__title">
						{{ displayTitle(notification) }}
					</button>
				</div>
				<div
					v-if="expandedIds.has(notification.id)"
					class="notification-item__body"
				>
					<NotificationContent :content="notification.content" />
					<blockquote
						v-if="notification.originalContent"
						class="notification-item__quote"
					>
						{{ notification.originalContent }}
					</blockquote>
					<div class="notification-item__actions">
						<time>{{ formatDateTime(notification.createdAt) }}</time>
						<button
							v-if="notification.type === 'COMMENT_REPLY'"
							type="button"
							:disabled="
								!notification.canInteract || updatingLikes.has(notification.id)
							"
							:aria-label="notification.liked ? '取消点赞' : '点赞回复'"
							@click.stop="toggleLike(notification)"
						>
							<i-lucide-thumbs-up
								:class="{ 'text-(--app-main)': notification.liked }"
							/>
							{{ notification.likeCount }}
						</button>
						<button
							v-if="
								notification.type === NOTIFICATION_TYPE.COMMENT_REPLY &&
								notification.canInteract
							"
							type="button"
							@click.stop="openReply(notification)"
						>
							<i-lucide-message-circle-reply />
							快捷回复
						</button>
					</div>
				</div>
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
import { useRouter } from 'vue-router';
import { formatDateTime } from '@/utils/datetime';
import { useNotifications } from '../composables/useNotifications';
import { NOTIFICATION_TYPE, type NotificationItem } from '../types/notification';
import NotificationContent from './NotificationContent.vue';

defineOptions({ name: 'NotificationDialog' });

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();
const router = useRouter();

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
	toggleLike,
	updatingLikes,
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

function displayTitle(notification: NotificationItem) {
	if (notification.authorName && notification.type === 'COMMENT_REPLY') {
		return `${notification.authorName} 回复了我的评论`;
	}
	if (notification.authorName && notification.type === 'MESSAGE_REPLY') {
		return `${notification.authorName} 回复了我的留言`;
	}
	return notification.title;
}

function openSource(notification: NotificationItem) {
	if (notification.type === 'COMMENT_REPLY' && notification.articleId) {
		void markRead(notification).catch(() => {});
		emit('update:modelValue', false);
		router.push({
			name: 'ArticleDetail',
			params: { articleId: notification.articleId },
			hash: '#article-comments',
			query: { replyId: String(notification.sourceId) },
		});
	} else if (notification.type === 'MESSAGE_REPLY' && notification.parentId) {
		void markRead(notification).catch(() => {});
		emit('update:modelValue', false);
		router.push({
			name: 'MessageBoard',
			query: {
				messageId: String(notification.parentId),
			},
		});
	}
}

function openReply(notification: NotificationItem) {
	if (notification.type === 'COMMENT_REPLY' && notification.articleId) {
		emit('update:modelValue', false);
		void router.push({
			name: 'ArticleDetail',
			params: { articleId: notification.articleId },
			hash: '#article-comments',
			query: { replyId: String(notification.sourceId) },
		});
	}
}
</script>

<style scoped lang="scss">
:global(.el-dialog.notification-dialog) {
	height: 60vh;
	min-height: 300px;
	display: flex;
	flex-direction: column;
}

:global(.el-dialog.notification-dialog .el-dialog__body) {
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
}

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

.notification-item__header {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	width: 100%;
	min-height: 3.5rem;
	padding: 0.75rem 0.25rem;
	background: var(--app-surface);
}

.notification-item__toggle,
.notification-item__title {
	border: 0;
	background: transparent;
	color: var(--app-text);
	text-align: left;
	cursor: pointer;
}

.notification-item--expanded .notification-item__header {
	position: sticky;
	top: 0;
	z-index: 1;
}

.notification-item__title {
	min-width: 0;
	flex: 1;
	font-weight: 600;
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

.notification-item__body {
	padding: 0 0.75rem 0.75rem 1.75rem;
}

.notification-item__body time {
	color: var(--app-text-muted);
	font-size: 0.75rem;
}

.notification-item__actions {
	display: flex;
	align-items: center;
	gap: 0.75rem;
	margin-top: 0.5rem;
}

.notification-item__actions button {
	display: flex;
	align-items: center;
	gap: 0.25rem;
	border: 0;
	background: transparent;
	color: var(--app-text-muted);
	cursor: pointer;
}

.notification-item__actions button:disabled {
	cursor: default;
	opacity: 0.5;
}

.notification-item__quote {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	line-clamp: 2;
	margin: 0.75rem 0;
	padding: 0.5rem 0.75rem;
	border-left: 2px solid var(--app-border);
	background: var(--app-router-hover);
	color: var(--app-text-muted);
	white-space: pre-wrap;
	overflow-wrap: anywhere;
	cursor: pointer;
}

.notification-dialog__more {
	display: block;
	margin: 1rem auto 0;
}

@media (width < 480px) {
	.notification-item__header {
		flex-wrap: wrap;
	}
}
</style>
