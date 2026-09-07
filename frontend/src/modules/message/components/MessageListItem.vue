<template>
	<article
		class="message-item"
		:class="{ 'message-item--muted': isMuted }"
	>
		<div
			class="message-item__header"
			:class="{ 'message-item__header--sticky': contentExpanded }"
		>
			<div class="message-item__identity">
				<AppUserAvatar
					:avatar-url="item.author?.avatarUrl"
					:name="item.author?.nickname || item.nickname"
					:user-id="item.author?.id"
					:size="34"
				/>
				<strong>{{ item.author?.nickname || item.nickname }}</strong>
				<el-tag
					v-if="item.status !== MESSAGE_STATUS.APPROVED"
					:size="'small'"
					:type="statusTagType(item.status)"
				>
					{{ statusLabel(item.status) }}
				</el-tag>
			</div>
			<div class="message-item__header-actions">
				<button
					v-if="contentOverflows"
					class="message-item__content-toggle"
					type="button"
					:aria-controls="`message-content-${item.id}`"
					:aria-expanded="contentExpanded"
					@click="toggleContent"
				>
					{{ contentExpanded ? '收起' : '展开' }}
				</button>
				<time>{{ formatDateTime(item.createdAt) }}</time>
			</div>
		</div>
		<p
			ref="contentRef"
			:id="`message-content-${item.id}`"
			class="message-item__content"
			:class="{ 'message-item__content--expanded': contentExpanded }"
		>
			{{ item.content }}
		</p>
		<p
			v-if="item.isMine && item.status === MESSAGE_STATUS.REJECTED && item.moderationReason"
			class="message-item__reason"
		>
			处理原因：{{ item.moderationReason }}
		</p>
		<div
			v-if="item.replies.length"
			class="message-item__replies"
		>
			<div
				v-for="reply in item.replies"
				:key="reply.id"
				class="message-reply"
			>
				<AppUserAvatar
					:avatar-url="reply.author?.avatarUrl"
					:name="reply.author?.nickname || '管理员'"
					:user-id="reply.author?.id"
					:size="28"
				/>
				<div>
					<div class="message-reply__meta">
						<strong>{{ reply.author?.nickname || '管理员' }}</strong>
						<time>{{ formatDateTime(reply.createdAt) }}</time>
					</div>
					<p>{{ reply.content }}</p>
				</div>
			</div>
		</div>
		<button
			v-if="item.isMine && item.status !== MESSAGE_STATUS.DELETED"
			class="message-item__delete"
			type="button"
			@click="emit('delete', item)"
		>
			<i-lucide-trash-2 />
			<span>删除</span>
		</button>
	</article>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { useResizeObserver } from '@vueuse/core';
import { formatDateTime } from '@/utils/datetime';
import type { PublicMessageItem, MessageStatus } from '../types/message';
import { MESSAGE_STATUS } from '../types/message';

defineOptions({
	name: 'MessageListItem',
});

const props = defineProps<{
	item: PublicMessageItem;
}>();

const emit = defineEmits<{
	delete: [item: PublicMessageItem];
}>();

const contentRef = ref<HTMLElement | null>(null);
const contentExpanded = ref(false);
const contentOverflows = ref(false);
const isMuted = computed(
	() =>
		props.item.status === MESSAGE_STATUS.REJECTED ||
		props.item.status === MESSAGE_STATUS.HIDDEN ||
		props.item.status === MESSAGE_STATUS.DELETED,
);

// 折叠状态下根据实际渲染高度判断是否需要展示“展开”按钮。
useResizeObserver(contentRef, () => {
	if (!contentRef.value || contentExpanded.value) return;
	contentOverflows.value = contentRef.value.scrollHeight > contentRef.value.clientHeight;
});

async function toggleContent() {
	contentExpanded.value = !contentExpanded.value;
	if (contentExpanded.value) return;
	await nextTick();
	if (contentRef.value) {
		contentOverflows.value = contentRef.value.scrollHeight > contentRef.value.clientHeight;
	}
}

function statusLabel(status: MessageStatus) {
	return {
		[MESSAGE_STATUS.PENDING]: '待审核',
		[MESSAGE_STATUS.APPROVED]: '已通过',
		[MESSAGE_STATUS.REJECTED]: '已拒绝',
		[MESSAGE_STATUS.HIDDEN]: '已隐藏',
		[MESSAGE_STATUS.DELETED]: '已删除',
	}[status];
}

function statusTagType(status: MessageStatus) {
	if (status === MESSAGE_STATUS.REJECTED || status === MESSAGE_STATUS.DELETED) return 'danger';
	if (status === MESSAGE_STATUS.PENDING) return 'warning';
	return 'info';
}
</script>

<style scoped lang="scss">
.message-item {
	padding: 0 0 2rem 0;
	// border-bottom: 1px solid var(--app-border);
}

.message-item--muted {
	color: var(--app-text-muted);
	opacity: 0.78;
}

.message-item__header,
.message-item__identity,
.message-reply__meta {
	display: flex;
	align-items: center;
}

.message-item__header {
	min-height: 2.125rem;
	justify-content: space-between;
	gap: 1rem;
	background-color: var(--app-message-item-header-bg);
	padding: 0.5rem 0.75rem;
	border-radius: 0 0 0.5rem 0.5rem;
}

.message-item__header--sticky {
	position: sticky;
	top: 0;
	z-index: 1;
}

.message-item__identity {
	min-width: 0;
	gap: 0.5rem;
}

.message-item__identity strong {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.message-item__header-actions {
	display: flex;
	flex: none;
	align-items: center;
	gap: 0.75rem;
}

.message-item__header time,
.message-reply__meta time {
	color: var(--app-text-muted);
	font-size: 0.9rem;
}

.message-item__content {
	max-height: 10rem;
	overflow: hidden;
	margin: 0.8rem 0 0;
	white-space: pre-wrap;
	word-break: break-word;
}

.message-item__content--expanded {
	max-height: none;
}

.message-item__content-toggle {
	padding: 0;
	border: 0;
	background: transparent;
	// color: var(--app-main);
	cursor: pointer;
	font-size: 1rem;
	font-weight: bold;
}

.message-item__content-toggle:hover,
.message-item__content-toggle:focus-visible {
	color: var(--app-button-hover);
}

.message-item__reason {
	margin-top: 0.5rem;
	color: var(--el-color-danger);
	font-size: 0.9rem;
}

.message-item__replies {
	margin-top: 1rem;
	margin-left: 1.5rem;
	padding-left: 1rem;
	border-left: 2px solid var(--app-border);
}

.message-reply {
	display: flex;
	gap: 0.6rem;
	padding-block: 0.5rem;
}

.message-reply p {
	margin: 0.25rem 0 0;
	white-space: pre-wrap;
}

.message-reply__meta {
	gap: 0.5rem;
}

.message-item__delete {
	display: inline-flex;
	align-items: center;
	gap: 0.3rem;
	margin-top: 0.5rem;
	border: 0;
	background: transparent;
	color: var(--app-text-muted);
	cursor: pointer;
	font-size: 0.9rem;
}

.message-item__delete:hover {
	color: var(--el-color-danger);
}

@media (max-width: 640px) {
	.message-item__header {
		align-items: flex-start;
	}

	.message-item__header-actions {
		align-items: flex-end;
		flex-direction: column;
		gap: 0.2rem;
	}
}
</style>
