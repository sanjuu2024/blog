<template>
	<ModalToolbar
		title="假名标注"
		modal-title="添加假名标注"
		:visible="visible"
		:disabled="disabled"
		width="min(28rem, calc(100vw - 2rem))"
		@onClick="open"
		@onClose="visible = false"
	>
		<template #trigger>
			<i-mdi-furigana-horizontal class="text-xl" />
		</template>
		<el-form
			ref="formRef"
			:model="form"
			:rules="rules"
			label-position="top"
			class="editor-ruby-form"
			@submit.prevent="submit"
		>
			<el-form-item
				label="原文"
				prop="original"
			>
				<el-input
					ref="originalInput"
					v-model="form.original"
					:disabled="disabled"
				/>
			</el-form-item>
			<el-form-item
				label="读音"
				prop="reading"
			>
				<el-input
					ref="readingInput"
					v-model="form.reading"
					placeholder="例如：かんじ"
					:disabled="disabled"
				/>
			</el-form-item>
			<div class="flex justify-end gap-2">
				<el-button @click="visible = false">取消</el-button>
				<el-button
					type="primary"
					native-type="submit"
					:disabled="disabled"
					>插入</el-button
				>
			</div>
		</el-form>
	</ModalToolbar>
</template>

<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue';
import { ModalToolbar, type ExposeParam } from 'md-editor-v3';
import {
	ElMessage,
	type FormInstance,
	type FormItemRule,
	type FormRules,
	type InputInstance,
} from 'element-plus';
import { createEditorRubyHtml } from '../utils/editorRuby';

const props = defineProps<{ editor?: ExposeParam; disabled?: boolean }>();
const visible = defineModel<boolean>('visible', { default: false });
const form = reactive({ original: '', reading: '' });
const formRef = ref<FormInstance>();
const originalInput = ref<InputInstance>();
const readingInput = ref<InputInstance>();
let insertRuby: (() => void) | undefined;
const validateSingleLine: NonNullable<FormItemRule['validator']> = (
	_rule,
	value: string,
	callback,
) => {
	if (!value.trim() || /[\r\n]/.test(value)) callback(new Error('请填写非空单行文本'));
	else callback();
};
const rules: FormRules<typeof form> = {
	original: [{ validator: validateSingleLine, trigger: 'blur' }],
	reading: [{ validator: validateSingleLine, trigger: 'blur' }],
};

async function open() {
	if (props.disabled || visible.value) return;
	const editor = props.editor;
	const view = editor?.getEditorView();
	if (!editor || !view) return;
	const selection = view.state.selection;
	const document = view.state.doc;
	form.original = view.state.sliceDoc(selection.main.from, selection.main.to);
	form.reading = '';
	// 弹窗会改变焦点，确认时恢复打开时的选区；外部替换正文后不使用旧位置。
	insertRuby = () => {
		if (editor.getEditorView() !== view || view.state.doc !== document) {
			ElMessage.warning('正文已变化，请重新添加标注');
			return;
		}
		view.dispatch({ selection });
		editor.insert(() => ({
			targetValue: createEditorRubyHtml(form.original, form.reading),
			select: false,
		}));
	};
	visible.value = true;
	await nextTick();
	formRef.value?.clearValidate();
	if (form.original) readingInput.value?.focus();
	else originalInput.value?.focus();
}

async function submit() {
	if (!visible.value || props.disabled || !formRef.value || !insertRuby) return;
	const valid = await formRef.value.validate().catch(() => false);
	if (!valid || props.disabled || !visible.value) return;
	insertRuby();
	visible.value = false;
	await nextTick();
	props.editor?.focus();
}

watch(
	visible,
	(isVisible, _, onCleanup) => {
		const content = props.editor?.getEditorView()?.contentDOM;
		if (!isVisible || !content) return;
		// 只锁定编辑内容，保留当前弹窗和取消操作；关闭或组件销毁时恢复原值。
		const originalInert = content.inert;
		content.inert = true;
		onCleanup(() => {
			content.inert = originalInert;
		});
	},
	{ flush: 'sync' },
);

watch(
	() => props.disabled,
	(disabled) => {
		if (disabled) visible.value = false;
	},
);
</script>

<style scoped lang="scss">
.editor-ruby-form {
	padding: 1rem;
}
</style>
