import { createRequire } from 'node:module';
import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, ref } from 'vue';
import { MdEditor, type ExposeParam } from 'md-editor-v3';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ElMessage } from 'element-plus';
import EditorRubyToolbar from './EditorRubyToolbar.vue';
import { RUBY_EDITOR_TOOLBARS } from '../utils/editorRuby';

const dependencyRequire = createRequire(createRequire(import.meta.url).resolve('md-editor-v3'));
const { undo, redo } = dependencyRequire('@codemirror/commands');

const Editor = defineComponent({
	components: { MdEditor, EditorRubyToolbar },
	props: { disabled: Boolean, initial: { type: String, default: '前文漢字後文' } },
	setup(props) {
		return {
			content: ref(props.initial),
			editor: ref<ExposeParam>(),
			visible: ref(false),
			toolbars: RUBY_EDITOR_TOOLBARS,
		};
	},
	template: `<MdEditor ref="editor" v-model="content" :toolbars="toolbars"
		:disabled="disabled" :preview="false"
		no-katex no-mermaid no-highlight no-prettier no-upload-img>
		<template #defToolbars><EditorRubyToolbar v-model:visible="visible" :editor="editor" :disabled="disabled" /></template>
	</MdEditor>`,
});

let wrapper: ReturnType<typeof mount<typeof Editor>>;

beforeEach(() => {
	const createRange = document.createRange.bind(document);
	vi.spyOn(document, 'createRange').mockImplementation(() => {
		const range = createRange();
		range.getClientRects = () => [] as unknown as DOMRectList;
		range.getBoundingClientRect = () => new DOMRect();
		return range;
	});
});

afterEach(() => {
	wrapper?.unmount();
	ElMessage.closeAll();
	vi.restoreAllMocks();
});

async function open(props: { disabled?: boolean; initial?: string } = {}) {
	wrapper = mount(Editor, { attachTo: document.body, props });
	await flushPromises();
	const editor = wrapper.vm.editor!;
	const view = editor.getEditorView()!;
	view.dispatch({ selection: { anchor: 2, head: Math.min(4, view.state.doc.length) } });
	await wrapper.get('button[title="假名标注"]').trigger('click');
	await flushPromises();
	return { editor, view };
}

async function fill(index: number, text: string) {
	const input = document.querySelectorAll<HTMLInputElement>('.editor-ruby-form input')[index]!;
	input.value = text;
	input.dispatchEvent(new Event('input', { bubbles: true }));
	await flushPromises();
}

async function submit() {
	document
		.querySelector<HTMLFormElement>('.editor-ruby-form')!
		.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
	await flushPromises();
}

describe('EditorRubyToolbar', () => {
	it('prefills selected text and inserts at the original selection with undo and redo', async () => {
		const { view } = await open();
		expect(document.querySelector<HTMLInputElement>('.editor-ruby-form input')!.value).toBe(
			'漢字',
		);
		expect(view.contentDOM.inert).toBe(true);
		view.dispatch({ selection: { anchor: 0 } });
		await fill(1, ' かんじ ');
		await submit();
		const result = '前文<ruby>漢字<rp>（</rp><rt>かんじ</rt><rp>）</rp></ruby>後文';
		await vi.waitFor(() => expect(view.state.doc.toString()).toBe(result));
		expect(wrapper.vm.visible).toBe(false);
		expect(view.contentDOM.inert).not.toBe(true);
		expect(undo(view)).toBe(true);
		expect(view.state.doc.toString()).toBe('前文漢字後文');
		expect(redo(view)).toBe(true);
		expect(view.state.doc.toString()).toBe(result);
	});

	it('escapes both fields and permits insertion at a cursor without selected text', async () => {
		const { view } = await open({ initial: '前文' });
		expect(document.querySelector<HTMLInputElement>('.editor-ruby-form input')!.value).toBe('');
		await fill(0, '<img src=x onerror="alert(1)">');
		await fill(1, '<script>&"</script>');
		await submit();
		const source = view.state.doc.toString();
		expect(source).toContain('&lt;img');
		expect(source).toContain('&lt;script&gt;&amp;&quot;&lt;/script&gt;');
		const preview = document.createElement('div');
		preview.innerHTML = source;
		expect(preview.querySelector('img, script')).toBeNull();
		expect(preview.querySelector('ruby rt')?.textContent).toBe('<script>&"</script>');
	});

	it('requires nonblank original text and reading before changing the document', async () => {
		const { view } = await open();
		await fill(0, ' ');
		await fill(1, ' ');
		await submit();
		expect(view.state.doc.toString()).toBe('前文漢字後文');
		expect(wrapper.vm.visible).toBe(true);
		await vi.waitFor(() =>
			expect(
				document.querySelectorAll('.editor-ruby-form .el-form-item__error'),
			).toHaveLength(2),
		);
	});

	it('cancels without changes and resets the previous reading on reopening', async () => {
		const { view } = await open();
		await fill(1, 'かんじ');
		const cancel = Array.from(
			document.querySelectorAll<HTMLButtonElement>('.editor-ruby-form button'),
		).find((button) => button.textContent?.trim() === '取消')!;
		cancel.click();
		await flushPromises();
		expect(view.state.doc.toString()).toBe('前文漢字後文');
		await wrapper.get('button[title="假名标注"]').trigger('click');
		await flushPromises();
		expect(
			document.querySelectorAll<HTMLInputElement>('.editor-ruby-form input')[1]!.value,
		).toBe('');
	});

	it('disables the entry and discards a pending annotation when uploads disable editing', async () => {
		const { view } = await open();
		await fill(1, 'かんじ');
		await wrapper.setProps({ disabled: true });
		expect(wrapper.vm.visible).toBe(false);
		expect(wrapper.get('button[title="假名标注"]').attributes('disabled')).toBeDefined();
		expect(view.state.doc.toString()).toBe('前文漢字後文');
	});

	it('does not replace a different document using a stale selection', async () => {
		const { view } = await open();
		await fill(1, 'かんじ');
		view.dispatch({
			changes: { from: 0, to: view.state.doc.length, insert: '外部更新后的正文' },
		});
		await submit();
		expect(view.state.doc.toString()).toBe('外部更新后的正文');
		expect(document.querySelector('.el-message')?.textContent).toContain('正文已变化');
	});
});
