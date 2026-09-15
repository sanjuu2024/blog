<template>
	<div class="reply-editor">
		<p class="reply-editor-title">回复 {{ targetName }}：</p>
		<el-input
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
defineOptions({
	name: 'PublicCommentReplyEditor',
});

const content = defineModel<string>({ required: true });

defineProps<{
	targetName: string;
	loading: boolean;
}>();

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
