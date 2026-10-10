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

		<!-- 切换通知的查看范围 -->
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
			class="notification-dialog__list-wrapper overflow-y-auto"
		>
			<div class="notification-dialog__list divide-y divide-(--app-border)">
				<div
					v-for="notification in notifications"
					:key="notification.id"
					class="notification-item"
				>
					<div class="notification-item__header">
						<span
							v-if="!notification.read"
							class="notification-item__unread"
						></span>
						<div class="notification-item__title">
							<span v-if="notification.type === NOTIFICATION_TYPE.ADMIN_MESSAGE"
								>管理员消息：{{ notification.title }}</span
							>
							<div v-else>
								<span class="notification-item__author mr-2 text-(--app-main)"
									>@{{ notification.authorName }}</span
								>
								<span v-if="notification.type === NOTIFICATION_TYPE.COMMENT_REPLY"
									>回复了我的评论</span
								>
								<span
									v-else-if="
										notification.type === NOTIFICATION_TYPE.MESSAGE_REPLY
									"
									>回复了我的留言</span
								>
							</div>
						</div>
						<time>{{ formatDateTime(notification.createdAt) }}</time>
					</div>

					<div class="notification-item__body">
						<div
							class="notification-item__content"
							@click.stop="openSource(notification)"
						>
							<NotificationContent :content="notification.content" />
						</div>
						<blockquote
							v-if="notification.originalContent"
							class="notification-item__quote"
						>
							{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}：{{
								notification.originalContent
							}}
						</blockquote>
					</div>
					<div
						class="notification-item__footer"
						v-if="notification.type === NOTIFICATION_TYPE.COMMENT_REPLY"
					>
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
							aria-label="快捷回复"
							@click.stop="openReply(notification)"
						>
							<i-lucide-message-circle-reply />
							快捷回复
						</button>
					</div>
					<PublicCommentReplyEditor
						v-if="isReplyEditorVisible(notification)"
						v-model="replyContent"
						:target-name="notification.authorName || ''"
						:loading="replySubmitting"
						@cancel="closeReplyEditor"
						@submit="submitReply(notification)"
					/>
				</div>
			</div>
			<AppLoadMoreTrigger
				:loading="loading"
				:has-next="page.hasNext"
				thing-str="通知"
				@load-more="loadNotifications(page.pageNum + 1)"
			/>
		</div>
	</el-dialog>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import AppLoadMoreTrigger from '@/components/AppLoadMoreTrigger.vue';
import { createComment } from '@/modules/comment/api/commentApi';
import PublicCommentReplyEditor from '@/modules/comment/components/PublicCommentReplyEditor.vue';
import { formatDateTime } from '@/utils/datetime';
import { useNotifications } from '../composables/useNotifications';
import { NOTIFICATION_TYPE, type NotificationItem } from '../types/notification';
import NotificationContent from './NotificationContent.vue';
import { useUserStore } from '@/stores/userStore.ts';

defineOptions({ name: 'NotificationDialog' });

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();
const router = useRouter();
const userStore = useUserStore();

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
const replyingNotificationId = ref<number | null>(null);
const replyContent = ref('');
const replySubmitting = ref(false);

watch(
	() => props.modelValue,
	(open) => {
		if (!open) return;
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

// 点击回复正文跳转到具体评论或留言；通知标题和操作按钮不触发跳转。
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

// 快捷回复编辑器只在当前通知项下展开，避免离开通知弹窗丢失上下文。
function isReplyEditorVisible(notification: NotificationItem) {
	return replyingNotificationId.value === notification.id;
}

// 快捷回复只支持评论回复，留言回复仍按当前留言板权限处理。
function openReply(notification: NotificationItem) {
	if (notification.type !== NOTIFICATION_TYPE.COMMENT_REPLY) return;
	void markRead(notification).catch(() => {});
	if (replyingNotificationId.value === notification.id) {
		closeReplyEditor();
		return;
	}
	replyingNotificationId.value = notification.id;
	replyContent.value = '';
}

function closeReplyEditor() {
	replyingNotificationId.value = null;
	replyContent.value = '';
}

async function submitReply(notification: NotificationItem) {
	const content = replyContent.value.trim();
	if (!content || !notification.articleId || !notification.sourceId) {
		ElMessage.warning('请输入回复内容');
		return;
	}
	replySubmitting.value = true;
	try {
		const result = await createComment(notification.articleId, {
			content,
			parentId: notification.sourceId,
		});
		closeReplyEditor();
		ElMessage.success(
			result.status === 'APPROVED' ? '回复发布成功' : '回复已提交，审核通过前仅自己可见',
		);
	} catch {
		// 请求错误由统一响应拦截器提示。
	} finally {
		replySubmitting.value = false;
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

.notification-dialog__list-wrapper {
	min-height: 8rem;
	max-height: 60dvh;
	overflow-y: auto;
}

.notification-item {
	padding: 1rem 0.5rem;
}

.notification-item__header {
	display: flex;
	align-items: center;
	gap: 0.5rem;
	width: 100%;
	background: var(--app-surface);

	.notification-item__unread {
		width: 0.45rem;
		height: 0.45rem;
		flex: none;
		border-radius: 50%;
		background: var(--app-main);
	}

	.notification-item__title {
		display: flex;
		color: var(--app-text);
		text-align: left;
		min-width: 0;
		flex: 1;
		font-weight: bold;
	}

	time {
		flex: none;
		color: var(--app-text-muted);
		font-size: 0.75rem;
	}
}

.notification-item__body {
	margin-top: 0.5rem;
	margin-left: 0.5rem;

	.notification-item__content {
		cursor: pointer;
	}

	.notification-item__quote {
		overflow: hidden;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		font-size: 0.85rem;
		margin: 0.5rem 0;
		padding: 0 0.5rem;
		border-left: 2px solid var(--app-border);
		color: var(--app-text-muted-more);
	}
}

.notification-item__footer {
	display: flex;
	align-items: center;
	gap: 0.75rem;
	font-size: 0.8rem;
	margin-top: 1rem;
	margin-left: 0.5rem;

	time {
		color: var(--app-text-muted);
	}

	button {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		border: 0;
		background: transparent;
		color: var(--app-text-muted);
		cursor: pointer;
		min-width: 30px;

		&:hover,
		&:focus-visible {
			color: var(--app-main);
		}
	}

	button:disabled {
		cursor: default;
		opacity: 0.5;
	}
}

@media (width < 480px) {
	.notification-item__header {
		flex-wrap: wrap;
	}
}
</style>
