import { escape } from 'lodash';
import { nextTick } from 'vue';
import type { ExposeParam, UploadImgCallBack } from 'md-editor-v3';
import type { UploadedImageData } from '../types/adminFile';

// 文件名和 URL 都进入 HTML 属性，必须转义，避免引号等字符改变标签结构。
export function createEditorImageHtml(image: Pick<UploadedImageData, 'url' | 'originalName'>) {
	return `<img src="${escape(image.url)}" alt="${escape(image.originalName)}" />`;
}

export async function finishEditorImageUpload(editor: ExposeParam, callback: UploadImgCallBack) {
	// insert 经编辑器事件异步提交，先等待 HTML 写入后再通知裁剪弹窗完成。
	await nextTick();
	const view = editor.getEditorView();
	if (!view) return;
	const selection = view.state.selection;
	// 空列表只完成上传流程；默认回调仍会调整选区，随后恢复 HTML 插入后的光标。
	callback([]);
	await nextTick();
	view.dispatch({ selection });
}
