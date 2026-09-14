<template>
	<div class="message-editor-wrapper">
		<!-- 移动端展开编辑框按钮 -->
		<el-button
			v-show="isMobile && !showEditor"
			type="primary"
			@click="showEditor = !showEditor"
			class="w-full"
		>
			<i-lucide-feather class="mr-1" />
			<span>写留言</span>
		</el-button>

		<div
			v-show="isMobile && showEditor"
			class="message-editor__toolbar"
		>
			<!-- 移动端下显示编辑框折叠的按钮 -->
			<el-button
				type="primary"
				@click="showEditor = !showEditor"
				class="message-editor__collapse-button"
				size="small"
			>
				<i-lucide-minimize2 class="mr-1" />
				<span>收起</span>
			</el-button>
		</div>

		<el-form
			v-show="!isMobile || showEditor"
			class="message-editor"
			:model="form"
			:rules="rules"
			label-position="right"
			label-width="auto"
			ref="formRef"
			@submit.prevent="handleSubmit"
		>
			<div class="message-editor__user-info-header">
				<div
					v-if="!props.isLogin"
					class="message-editor__guest-fields"
				>
					<el-form-item
						prop="nickname"
						label="昵称"
						required
						label-position="left"
						label-width="60px"
					>
						<el-input
							v-model="form.nickname"
							maxlength="20"
							show-word-limit
							placeholder="请输入昵称"
							aria-label="留言昵称"
						/>
					</el-form-item>
					<el-form-item
						prop="email"
						label="邮箱"
						label-position="left"
						label-width="60px"
						:required="form.notifyOnReply"
					>
						<el-input
							v-model="form.email"
							type="email"
							maxlength="255"
							:placeholder="form.notifyOnReply ? '请输入邮箱' : '邮箱（可选）'"
							aria-label="留言邮箱"
						/>
					</el-form-item>
				</div>
				<div
					v-else
					class="message-editor__login-hint"
				>
					<i-lucide-user-round />
					<span>已登录，将使用你的账号资料</span>
				</div>

				<label class="message-editor__notify">
					<el-checkbox
						v-model="form.notifyOnReply"
						@change="handleNotifyChange"
					>
						{{
							props.isLogin ? '有回复时通过账号邮箱提醒我' : '有回复时通过邮箱提醒我'
						}}
					</el-checkbox>
				</label>
			</div>
			<div class="message-editor__body">
				<el-form-item
					class="message-editor__content-form-item"
					prop="content"
					label="留言内容"
				>
					<el-input
						v-model="form.content"
						type="textarea"
						:rows="3"
						maxlength="1000"
						show-word-limit
						placeholder="写下你想说的话..."
						aria-label="留言内容"
					/>
				</el-form-item>
				<el-button
					type="primary"
					:loading="props.loading"
					:disabled="props.loading || !canSubmit"
					native-type="submit"
				>
					<i-lucide-send class="mr-1" />
					<span>发表留言</span>
				</el-button>
			</div>
		</el-form>
	</div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import type { FormInstance, FormItemRule, FormRules } from 'element-plus';
import { EMAIL_FORMAT_MESSAGE, EMAIL_FORMAT_PATTERN } from '@/constants/validation';
import type { CreateMessageRequest } from '../types/message';
import { useMediaQuery } from '@vueuse/core';

const isMobile = useMediaQuery('(width < 768px)');

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

const showEditor = ref(!isMobile.value);

const form = reactive({
	nickname: '',
	email: '',
	content: '',
	notifyOnReply: false,
});

const formRef = ref<FormInstance>();
const canSubmit = computed(() => {
	if (!form.content.trim()) return false;
	if (props.isLogin) return true;
	if (!form.nickname.trim()) return false;
	if (!form.notifyOnReply) return true;
	return EMAIL_FORMAT_PATTERN.test(form.email.trim());
});

const rules: FormRules = {
	nickname: [
		{
			required: !props.isLogin,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				if (props.isLogin || value?.trim()) {
					callback();
					return;
				}
				callback(new Error('请输入昵称'));
			},
		},
	],
	email: [
		{
			required: !props.isLogin && form.notifyOnReply,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				const email = value?.trim() || '';
				if (props.isLogin || (!form.notifyOnReply && !email)) {
					callback();
					return;
				}
				if (!email) {
					callback(new Error('勾选回复通知时必须填写邮箱'));
					return;
				}
				if (!EMAIL_FORMAT_PATTERN.test(email)) {
					callback(new Error(EMAIL_FORMAT_MESSAGE));
					return;
				}
				callback();
			},
		},
	],
	content: [
		{
			required: true,
			trigger: 'change',
			validator: (_rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
				if (!value?.trim()) {
					callback(new Error('请输入留言内容'));
					return;
				}
				callback();
			},
		},
	],
};

async function handleSubmit() {
	if (props.loading) return;

	try {
		await formRef.value?.validate();
	} catch {
		return;
	}

	const data: CreateMessageRequest = {
		nickname: form.nickname.trim() || undefined,
		email: form.email.trim() || undefined,
		content: form.content.trim(),
		notifyOnReply: form.notifyOnReply,
	};
	emit('submit', data);
}

function handleNotifyChange() {
	if (!props.isLogin) {
		formRef.value?.validateField('email').catch(() => undefined);
	}
}

function clear() {
	form.nickname = '';
	form.email = '';
	form.content = '';
	form.notifyOnReply = false;
	formRef.value?.clearValidate();
	formRef.value?.resetFields();
}

defineExpose({ clear });
</script>

<style scoped lang="scss">
.message-editor-wrapper {
	width: 100%;
}

.message-editor__toolbar {
	display: flex;
	justify-content: flex-end;
	margin-bottom: 0.75rem;
}

.message-editor__user-info-header {
	display: flex;
	gap: 1rem;
}

.message-editor {
	padding: 1rem;
	border: 1px solid var(--app-border);
	border-radius: 0.3rem;
	background-color: var(--app-surface);
}

.message-editor__guest-fields {
	:deep(.el-form-item__label-wrap) {
		margin-left: 0;
	}

	display: flex;
	align-items: center;
	gap: 0.75rem;
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

.message-editor__content-form-item {
	flex: 1;
	margin-bottom: 0;
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

	.message-editor__user-info-header {
		flex-direction: column;
		align-items: stretch;
		gap: 0;
	}
}
</style>
