import { describe, expect, it } from 'vitest';
import { renderImageCaptions } from './renderImageCaptions';

describe('renderImageCaptions', () => {
	it('keeps image nodes, surrounding text, and image links while adding individual captions', () => {
		const container = document.createElement('div');
		container.innerHTML =
			'<p>前文<img src="/first.png" alt="第一张" title="第一张图注" width="320" height="180">后文</p>' +
			'<p><a href="https://example.com/image" title="链接说明" target="_blank"><img src="/second.png" title="第二张图注"></a></p>';
		const images = Array.from(container.querySelectorAll('img'));
		const link = container.querySelector('a')!;

		renderImageCaptions(container);

		expect(Array.from(container.querySelectorAll('img'))).toEqual(images);
		expect(images[0].getAttribute('alt')).toBe('第一张');
		expect(images[0].getAttribute('width')).toBe('320');
		expect(images[0].getAttribute('height')).toBe('180');
		expect(container.querySelectorAll('img[title]')).toHaveLength(0);
		expect(container.querySelector('p')?.textContent).toBe('前文第一张图注后文');
		expect(link.querySelector('img')).toBe(images[1]);
		expect(link.getAttribute('href')).toBe('https://example.com/image');
		expect(link.getAttribute('title')).toBe('链接说明');
		expect(link.getAttribute('target')).toBe('_blank');
		expect(link.nextElementSibling?.textContent).toBe('第二张图注');
	});

	it('ignores absent or blank titles and never falls back to alt text', () => {
		const container = document.createElement('div');
		container.innerHTML =
			'<p><img src="/first.png" alt="替代文本"><img src="/second.png" alt="另一个替代文本" title="  "><img src="/third.png" title=""></p>';

		renderImageCaptions(container);

		expect(container.querySelectorAll('.article-image')).toHaveLength(0);
		expect(container.querySelectorAll('img')).toHaveLength(3);
		expect(container.querySelectorAll('img[title]')).toHaveLength(0);
	});

	it('uses plain text for captions and does not add duplicates when processed again', () => {
		const container = document.createElement('div');
		const image = document.createElement('img');
		const title = '<img src=x onerror=alert(1)> & $x^2$';
		image.setAttribute('title', title);
		container.append(image);

		renderImageCaptions(container);
		renderImageCaptions(container);

		const caption = container.querySelector('.article-image__caption')!;
		expect(caption.textContent).toBe(title);
		expect(caption.childElementCount).toBe(0);
		expect(container.querySelectorAll('.article-image')).toHaveLength(1);
		expect(container.querySelectorAll('img')).toHaveLength(1);
	});
});
