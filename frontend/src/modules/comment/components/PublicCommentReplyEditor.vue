<template>
	<div class="reply-editor">
		<p class="reply-editor-title">回复 {{ targetName }}：</p>
		<el-checkbox
			class="mb-2"
			v-model="notifyOnReply"
			:disabled="emailVerified === false"
		>
			<span>有新的回复时通过账号邮箱提醒我</span>
			<span v-show="emailVerified === false">（邮箱未验证）</span>
		</el-checkbox>
		<el-input
			ref="inputRef"
			v-model.trim="content"
			type="textarea"
			:rows="3"
			maxlength="1000"
			show-word-limit
			placeholder="写下你的回复吧"
		/>
		<div class="mt-2 flex justify-end">
			<el-button @click="emit('cancel')">取消</el-button>
			<el-button
				type="primary"
				:loading="loading"
				@click="emit('submit')"
			>
				发表回复
			</el-button>
		</div>
	</div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';
import type { InputInstance } from 'element-plus';

defineOptions({
	name: 'PublicCommentReplyEditor',
});

const content = defineModel<string>({ required: true });
const notifyOnReply = defineModel<boolean>('notifyOnReply', { default: false });

const { emailVerified, autofocus = false } = defineProps<{
	targetName: string;
	loading: boolean;
	emailVerified?: boolean;
	autofocus?: boolean;
}>();

const inputRef = ref<InputInstance>();
onMounted(async () => {
	if (!autofocus) return;
	await nextTick();
	// 焦点只在编辑器首次渲染时设置；滚动由文章定位逻辑处理，避免两者抢位置。
	inputRef.value?.textarea?.focus({ preventScroll: true });
});

const emit = defineEmits<{
	cancel: [];
	submit: [];
}>();
</script>

<style scoped lang="scss">
.reply-editor {
	margin-top: 0.75rem;
	padding: 0.75rem;
	border-radius: 0.75rem;
}

.reply-editor-title {
	margin-bottom: 0.5rem;
	color: var(--app-text-muted);
	font-size: 0.9rem;
}
</style>
