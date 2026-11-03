// 在安全正文 DOM 上生成图注，不改写接口返回的 HTML 或 Markdown。
export function renderImageCaptions(container: HTMLElement) {
	container.querySelectorAll<HTMLImageElement>('img[title]').forEach((image) => {
		const title = image.getAttribute('title')?.trim();
		image.removeAttribute('title');
		if (!title) {
			return;
		}

		const wrapper = document.createElement('span');
		wrapper.className = 'article-image';
		const caption = document.createElement('span');
		caption.className = 'article-image__caption';
		// title 可能包含尖括号等文本，不能作为 HTML 插入。
		caption.textContent = title;

		// 单独包裹图片的链接一起保留，让图注位于链接之外；span 也可合法放在段落内。
		const parent = image.parentElement;
		const content =
			parent?.tagName === 'A' && parent.childElementCount === 1 && !parent.textContent?.trim()
				? parent
				: image;
		content.replaceWith(wrapper);
		wrapper.append(content, caption);
	});
}
