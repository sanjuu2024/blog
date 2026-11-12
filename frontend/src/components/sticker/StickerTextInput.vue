<template>
	<div class="sticker-text-input">
		<el-input
			ref="inputRef"
			v-model="content"
			type="textarea"
			:rows="rows"
			:maxlength="STICKER_CONTENT_RAW_MAX_LENGTH"
			:placeholder="placeholder"
			:disabled="disabled"
			:aria-label="`${contentLabel}内容`"
			@input="rememberSelection"
			@blur="rememberSelection"
			@select="rememberSelection"
			@keyup="rememberSelection"
			@click="rememberSelection"
		/>
		<div class="sticker-text-input__toolbar">
			<div class="sticker-text-input__toolbar-left">
				<el-button
					class="sticker-text-input__toggle"
					aria-label="选择表情"
					:aria-expanded="pickerVisible"
					:disabled="disabled"
					@mousedown.prevent
					@click="togglePicker"
				>
					<i-solar-emoji-funny-circle-outline class="text-lg" />
				</el-button>
				<span
					class="sticker-text-input__count"
					:class="{
						'sticker-text-input__count--invalid': length > STICKER_CONTENT_MAX_LENGTH,
					}"
				>
					字数 {{ length }} / {{ STICKER_CONTENT_MAX_LENGTH }}
				</span>
				<span
					class="sticker-text-input__count"
					:class="{
						'sticker-text-input__count--invalid': stickerCount > STICKER_MAX_COUNT,
					}"
				>
					表情 {{ stickerCount }} / {{ STICKER_MAX_COUNT }}
				</span>
			</div>
			<div class="sticker-text-input__actions"><slot name="actions" /></div>
		</div>
		<StickerPicker
			v-if="pickerVisible && !disabled"
			@select="insertSticker"
			@close="pickerVisible = false"
		/>
	</div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { ElMessage, type InputInstance } from 'element-plus';
import { useStickers } from '@/composables/useStickers';
import {
	STICKER_CONTENT_MAX_LENGTH,
	STICKER_MAX_COUNT,
	STICKER_CONTENT_RAW_MAX_LENGTH,
	countStickerContentLength,
	countStickers,
	insertStickerToken,
	getStickerContentError,
} from '@/utils/sticker';
import type { Sticker } from '@/types/sticker';
import StickerPicker from '@/components/sticker/StickerPicker.vue';

const content = defineModel<string>({ required: true });
const props = withDefaults(
	defineProps<{
		rows?: number;
		placeholder?: string;
		contentLabel?: string;
		disabled?: boolean;
	}>(),
	{
		rows: 3,
		placeholder: '检测到访客信号......等待评论电波接入！ヾ(≧▽≦*)o',
		contentLabel: '评论',
		disabled: false,
	},
);
const { stickers } = useStickers();
const length = computed(() => countStickerContentLength(content.value.trim(), stickers.value));
const stickerCount = computed(() => countStickers(content.value, stickers.value));
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

async function insertSticker(sticker: Sticker) {
	if (props.disabled) return;
	const { start, end } = selection || { start: content.value.length, end: content.value.length };
	const inserted = insertStickerToken(content.value, sticker.token, start, end);
	const error = getStickerContentError(inserted.content, stickers.value, props.contentLabel);
	if (error) {
		ElMessage.warning(error);
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
.sticker-text-input {
	width: 100%;
}

.sticker-text-input__toolbar {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 0.65rem;
	margin-top: 0.65rem;

	.sticker-text-input__toolbar-left {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 0.5rem;
	}

	.sticker-text-input__actions {
		display: flex;
		margin-left: auto;
		flex-wrap: wrap;
		justify-content: flex-end;
		gap: 0.5rem;
	}
}

.sticker-text-input__toggle {
	padding: 0.25rem;
	border-radius: 0.25rem;

	&:hover,
	&:focus-visible {
		background: var(--app-router-hover);
		color: var(--app-main);
	}
}

.sticker-text-input__count {
	color: var(--app-text-muted);
	font-size: 0.8rem;
}

.sticker-text-input__count--invalid {
	color: var(--el-color-danger);
}
</style>
