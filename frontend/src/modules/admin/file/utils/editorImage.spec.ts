import { createRequire } from 'node:module';
import { flushPromises, mount } from '@vue/test-utils';
import { config, MdEditor, type ExposeParam } from 'md-editor-v3';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createEditorImageHtml, finishEditorImageUpload } from './editorImage';

beforeEach(() => {
	// jsdom 缺少文本范围尺寸接口，补空布局数据以验证真实编辑器事务和裁剪上传。
	const createRange = document.createRange.bind(document);
	vi.spyOn(document, 'createRange').mockImplementation(() => {
		const range = createRange();
		range.getClientRects = () => [] as unknown as DOMRectList;
		range.getBoundingClientRect = () => new DOMRect();
		return range;
	});
});

afterEach(() => vi.restoreAllMocks());

describe('createEditorImageHtml', () => {
	it('keeps URL and filename inside their attributes without adding executable markup', () => {
		const html = createEditorImageHtml({
			url: 'https://img.example.com/a.png?x=1&y="value"',
			originalName: '图" onerror="alert(1)<script>&\'.png',
		});
		const container = document.createElement('div');
		container.innerHTML = html;
		const img = container.querySelector('img')!;

		expect(html).toMatch(/^<img src=".*" alt=".*" \/>$/);
		expect(img.getAttribute('src')).toBe('https://img.example.com/a.png?x=1&y="value"');
		expect(img.getAttribute('alt')).toBe('图" onerror="alert(1)<script>&\'.png');
		expect(img.getAttributeNames()).toEqual(['src', 'alt']);
		expect(container.children).toHaveLength(1);
	});

	it('inserts HTML at the disabled editor selection and preserves undo and redo after unlocking', async () => {
		const dependencyRequire = createRequire(
			createRequire(import.meta.url).resolve('md-editor-v3'),
		);
		const wrapper = mount(MdEditor, {
			attachTo: document.body,
			props: {
				modelValue: '前文选中后文',
				preview: false,
				toolbars: [],
				noKatex: true,
				noMermaid: true,
				noHighlight: true,
				noPrettier: true,
				noUploadImg: true,
			},
		});
		try {
			await flushPromises();
			const editor = wrapper.vm as unknown as ExposeParam;
			const view = editor.getEditorView()!;
			expect(view).toBeDefined();
			view.dispatch({ selection: { anchor: 2, head: 4 } });
			await wrapper.setProps({ disabled: true });
			expect(wrapper.get('.cm-content').attributes('contenteditable')).toBe('false');
			const html = createEditorImageHtml({
				url: 'https://img.example.com/a.png',
				originalName: '图.png',
			});
			editor.insert(() => ({ targetValue: `${html}\n\n`, select: false }));
			await vi.waitFor(() => expect(view.state.doc.toString()).toBe(`前文${html}\n\n后文`));

			await wrapper.setProps({ disabled: false });
			const { undo, redo } = dependencyRequire('@codemirror/commands');
			expect(undo(view)).toBe(true);
			expect(view.state.doc.toString()).toBe('前文选中后文');
			expect(redo(view)).toBe(true);
			expect(view.state.doc.toString()).toBe(`前文${html}\n\n后文`);
		} finally {
			wrapper.unmount();
		}
	});

	it('closes crop uploads after inserting HTML once and preserves the cursor and undo history', async () => {
		const dependencyRequire = createRequire(
			createRequire(import.meta.url).resolve('md-editor-v3'),
		);
		const cropperReady = vi.fn();
		class TestCropper {
			constructor() {
				cropperReady();
			}
			clear() {}
			destroy() {}
			getCroppedCanvas() {
				return { toDataURL: () => 'data:image/png;base64,iVBORw0KGgo=' };
			}
		}
		config({ editorExtensions: { cropper: { instance: TestCropper } } });
		const html = createEditorImageHtml({
			url: 'https://img.example.com/crop.png',
			originalName: '裁剪.png',
		});
		let editor!: ExposeParam;
		let completion: Promise<void> | undefined;
		const upload = vi.fn((_files, callback) => {
			editor.insert(() => ({ targetValue: `${html}\n\n`, select: false }));
			completion = finishEditorImageUpload(editor, callback);
		});
		const wrapper = mount(MdEditor, {
			attachTo: document.body,
			props: {
				modelValue: '前文选中后文',
				toolbars: ['image'],
				preview: false,
				noKatex: true,
				noMermaid: true,
				noHighlight: true,
				noPrettier: true,
				onUploadImg: upload,
			},
		});
		try {
			await flushPromises();
			editor = wrapper.vm as unknown as ExposeParam;
			const view = editor.getEditorView()!;
			view.dispatch({ selection: { anchor: 2, head: 4 } });
			await wrapper
				.getComponent({ name: 'ToolbarImageDropdown' })
				.get('button')
				.trigger('click');
			await flushPromises();
			document.querySelectorAll<HTMLElement>('.md-editor-menu-item-image')[2].click();
			await flushPromises();
			const clip = wrapper.getComponent({ name: 'md-editor-modal-clip' });
			expect(clip.props('visible')).toBe(true);
			const fileInput = document.querySelector<HTMLInputElement>(
				'.md-editor-modal-clip input[type="file"]',
			)!;
			Object.defineProperty(fileInput, 'files', {
				value: [new File(['image'], 'image.png', { type: 'image/png' })],
			});
			fileInput.dispatchEvent(new Event('change', { bubbles: true }));
			await vi.waitFor(() => expect(cropperReady).toHaveBeenCalledOnce());
			document
				.querySelector<HTMLButtonElement>(
					'.md-editor-modal-clip .md-editor-form-item button',
				)!
				.click();
			await flushPromises();
			await completion;

			expect(upload).toHaveBeenCalledOnce();
			expect(clip.props('visible')).toBe(false);
			expect(view.state.doc.toString()).toBe(`前文${html}\n\n后文`);
			expect(view.state.selection.main.from).toBe(2 + html.length + 2);
			const { undo, redo } = dependencyRequire('@codemirror/commands');
			expect(undo(view)).toBe(true);
			expect(view.state.doc.toString()).toBe('前文选中后文');
			expect(redo(view)).toBe(true);
			expect(view.state.doc.toString()).toBe(`前文${html}\n\n后文`);
		} finally {
			wrapper.unmount();
			config({ editorExtensions: { cropper: { instance: undefined } } });
		}
	});
});
