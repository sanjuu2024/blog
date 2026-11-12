<template>
	<div class="comment-text-input">
		<el-input
			ref="inputRef"
			v-model="content"
			type="textarea"
			:rows="rows"
			:maxlength="COMMENT_RAW_MAX_LENGTH"
			:placeholder="placeholder"
			@input="rememberSelection"
			@blur="rememberSelection"
			@select="rememberSelection"
			@keyup="rememberSelection"
			@click="rememberSelection"
		/>
		<div class="comment-text-input__toolbar">
			<el-button
				class="comment-text-input__toggle"
				aria-label="选择表情"
				:aria-expanded="pickerVisible"
				@mousedown.prevent
				@click="togglePicker"
			>
				<i-solar-emoji-funny-circle-outline class="text-lg" />
			</el-button>
			<span
				class="comment-text-input__count"
				:class="{ 'comment-text-input__count--invalid': length > COMMENT_MAX_LENGTH }"
			>
				字数 {{ length }} / {{ COMMENT_MAX_LENGTH }}
			</span>
			<span
				class="comment-text-input__count"
				:class="{
					'comment-text-input__count--invalid': stickerCount > COMMENT_MAX_STICKERS,
				}"
			>
				表情 {{ stickerCount }} / {{ COMMENT_MAX_STICKERS }}
			</span>
			<div class="comment-text-input__actions"><slot name="actions" /></div>
		</div>
		<CommentStickerPicker
			v-if="pickerVisible"
			@select="insertSticker"
			@close="pickerVisible = false"
		/>
	</div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { ElMessage, type InputInstance } from 'element-plus';
import { useCommentStickers } from '../composables/useCommentStickers';
import {
	COMMENT_MAX_LENGTH,
	COMMENT_MAX_STICKERS,
	COMMENT_RAW_MAX_LENGTH,
	countCommentLength,
	countCommentStickers,
	insertCommentSticker,
} from '../utils/sticker';
import type { CommentSticker } from '../types/sticker';
import CommentStickerPicker from './CommentStickerPicker.vue';

const content = defineModel<string>({ required: true });
withDefaults(defineProps<{ rows?: number; placeholder?: string }>(), {
	rows: 3,
	placeholder: '检测到访客信号......等待评论电波接入！ヾ(≧▽≦*)o',
});
const { stickers } = useCommentStickers();
const length = computed(() => countCommentLength(content.value.trim(), stickers.value));
const stickerCount = computed(() => countCommentStickers(content.value, stickers.value));
const inputRef = ref<InputInstance>();
const pickerVisible = ref(false);
let selection: { start: number; end: number } | null = null;

function rememberSelection() {
	const textarea = inputRef.value?.textarea;
	if (textarea) selection = { start: textarea.selectionStart, end: textarea.selectionEnd };
}

function focus() {
	inputRef.value?.textarea?.focus({ preventScroll: true });
}

function togglePicker() {
	rememberSelection();
	pickerVisible.value = !pickerVisible.value;
}

async function insertSticker(sticker: CommentSticker) {
	const { start, end } = selection || { start: content.value.length, end: content.value.length };
	const inserted = insertCommentSticker(content.value, sticker.token, start, end);
	if (countCommentStickers(inserted.content, stickers.value) > COMMENT_MAX_STICKERS) {
		ElMessage.warning('每条评论最多添加 20 个表情');
		return;
	}
	if (
		inserted.content.length > COMMENT_RAW_MAX_LENGTH ||
		countCommentLength(inserted.content.trim(), stickers.value) > COMMENT_MAX_LENGTH
	) {
		ElMessage.warning('评论内容不能超过 1000 个字符');
		return;
	}
	content.value = inserted.content;
	await nextTick();
	focus();
	inputRef.value?.textarea?.setSelectionRange(inserted.caret, inserted.caret);
	selection = { start: inserted.caret, end: inserted.caret };
}

defineExpose({ focus });
</script>

<style scoped lang="scss">
.comment-text-input__toolbar {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 0.65rem;
	margin-top: 0.65rem;
}

.comment-text-input__toggle {
	padding: 0.25rem 0.5rem;
	border-radius: 0.25rem;

	&:hover,
	&:focus-visible {
		background: var(--app-router-hover);
		color: var(--app-main);
	}
}

.comment-text-input__count {
	color: var(--app-text-muted);
	font-size: 0.8rem;
}

.comment-text-input__count--invalid {
	color: var(--el-color-danger);
}

.comment-text-input__actions {
	display: flex;
	margin-left: auto;
	flex-wrap: wrap;
	justify-content: flex-end;
	gap: 0.5rem;
}
</style>
