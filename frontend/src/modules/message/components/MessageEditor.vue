<template>
	<form
		class="message-editor"
		@submit.prevent="handleSubmit"
	>
		<div
			v-if="!props.isLogin"
			class="message-editor__guest-fields"
		>
			<el-input
				v-model="nickname"
				maxlength="20"
				show-word-limit
				placeholder="你的昵称"
				aria-label="留言昵称"
			/>
			<el-input
				v-model="email"
				type="email"
				maxlength="255"
				placeholder="邮箱（可选）"
				aria-label="留言邮箱"
			/>
		</div>
		<div
			v-else
			class="message-editor__login-hint"
		>
			<i-lucide-user-round />
			<span>已登录，将使用你的账号资料</span>
		</div>
		<label class="message-editor__notify">
			<el-checkbox v-model="notifyOnReply">
				{{ props.isLogin ? '有回复时通过账号邮箱提醒我' : '有回复时通过邮箱提醒我' }}
			</el-checkbox>
		</label>
		<div class="message-editor__body">
			<el-input
				v-model="content"
				type="textarea"
				:rows="3"
				maxlength="1000"
				show-word-limit
				placeholder="写下你想说的话..."
				aria-label="留言内容"
			/>
			<el-button
				type="primary"
				:loading="props.loading"
				:disabled="!canSubmit"
				native-type="submit"
			>
				<i-lucide-send />
				<span>发表留言</span>
			</el-button>
		</div>
	</form>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { CreateMessageRequest } from '../types/message';

defineOptions({
	name: 'MessageEditor',
});

const props = withDefaults(
	defineProps<{
		isLogin: boolean;
		loading?: boolean;
	}>(),
	{
		loading: false,
	},
);

const emit = defineEmits<{
	submit: [data: CreateMessageRequest];
}>();

const nickname = ref('');
const email = ref('');
const content = ref('');
const notifyOnReply = ref(false);
const canSubmit = computed(() => {
	if (!content.value.trim()) return false;
	if (props.isLogin) return true;
	if (!nickname.value.trim()) return false;
	return !notifyOnReply.value || Boolean(email.value.trim());
});

function handleSubmit() {
	if (!canSubmit.value) return;
	const data: CreateMessageRequest = {
		nickname: nickname.value.trim() || undefined,
		email: email.value.trim() || undefined,
		content: content.value.trim(),
		notifyOnReply: notifyOnReply.value,
	};
	emit('submit', data);
}

function clear() {
	nickname.value = '';
	email.value = '';
	content.value = '';
	notifyOnReply.value = false;
}

defineExpose({ clear });
</script>

<style scoped lang="scss">
.message-editor {
	padding: 1rem;
	border: 1px solid var(--app-border);
	border-radius: 0.3rem;
	background-color: var(--app-surface);
}

.message-editor__guest-fields {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 0.75rem;
	margin-bottom: 0.75rem;
}

.message-editor__notify {
	display: block;
	margin: 0 0 0.75rem;
	color: var(--app-text-muted);
	font-size: 0.9rem;
}

.message-editor__login-hint {
	display: flex;
	align-items: center;
	gap: 0.4rem;
	margin-bottom: 0.75rem;
	color: var(--app-text-muted);
	font-size: 0.9rem;
}

.message-editor__body {
	display: flex;
	align-items: flex-end;
	gap: 0.75rem;
}

.message-editor__body :deep(.el-textarea) {
	flex: 1;
}

.message-editor__body .el-button {
	flex: none;
}

@media (width < 768px) {
	.message-editor__guest-fields,
	.message-editor__body {
		display: flex;
		flex-direction: column;
		align-items: stretch;
	}
}
</style>
