<template>
	<section class="message-board">
		<div class="message-board__list">
			<header class="message-board__header">
				<i-lucide-message-circle-more class="message-board__icon" />
				<h1>留言板</h1>
			</header>
			<div class="message-board__scroll app-scrollbar app-scrollbar--stable">
				<div
					v-if="!messageList.length && !loading"
					class="message-board__empty"
				>
					还没有留言，来说点什么吧。
				</div>
				<MessageListItem
					v-for="item in messageList"
					:key="item.id"
					:item="item"
					@delete="removeMessage"
				/>
				<AppLoadMoreTrigger
					:loading="loading"
					:has-next="pageParams.hasNext"
					:show-no-more="messageList.length > 0"
					thing-str="留言"
					@load-more="loadMoreMessages"
				/>
			</div>
		</div>
		<MessageEditor
			ref="editorRef"
			:is-login="authStore.isLogin"
			:loading="submitting"
			@submit="handleSubmit"
		/>
	</section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useUserStore } from '@/stores/userStore';
import MessageEditor from '../components/MessageEditor.vue';
import MessageListItem from '../components/MessageListItem.vue';
import { usePublicMessageList } from '../composables/usePublicMessageList';
import type { CreateMessageRequest } from '../types/message';

defineOptions({
	name: 'MessageBoardPage',
});

const authStore = useAuthStore();
const userStore = useUserStore();
const editorRef = ref<InstanceType<typeof MessageEditor> | null>(null);
const submitting = ref(false);
const {
	messageList,
	loading,
	pageParams,
	getMessageList,
	loadMoreMessages,
	submitMessage,
	removeMessage,
} = usePublicMessageList();

onMounted(() => {
	getMessageList();
});

watch(
	() => userStore.userInfo?.id ?? null,
	() => {
		// 身份变化后立即移除旧列表，避免退出登录后短暂保留本人未公开留言。
		messageList.value = [];
		getMessageList(1);
	},
);

async function handleSubmit(form: CreateMessageRequest) {
	if (submitting.value) return;
	submitting.value = true;
	try {
		await submitMessage(form);
		editorRef.value?.clear();
	} finally {
		submitting.value = false;
	}
}
</script>

<style scoped lang="scss">
.message-board {
	display: flex;
	height: 100%;
	min-height: 0;
	flex-direction: column;
	gap: 1rem;
}

.message-board__list {
	display: flex;
	min-height: 0;
	flex: 1;
	flex-direction: column;
	padding: 0;
	border: none;
	border-radius: 0;
	background: transparent;
}

.message-board__header {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 0.6rem;
	padding-bottom: 1rem;
}

.message-board__header h1 {
	margin: 0;
	font-size: 1.6rem;
}

.message-board__icon {
	width: 2rem;
	height: 2rem;
	color: var(--app-main);
}

.message-board__scroll {
	min-height: 0;
	flex: 1;
	overflow-y: auto;
	padding-inline: 0.5rem;
	scrollbar-gutter: stable both-edges;
	scrollbar-width: auto;
}

.message-board__scroll::-webkit-scrollbar {
	width: 10px;
}

.message-board__empty {
	display: grid;
	min-height: 12rem;
	place-items: center;
	color: var(--app-text-muted);
}
</style>
