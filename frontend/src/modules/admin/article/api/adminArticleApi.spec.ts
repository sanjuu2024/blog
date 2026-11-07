import { describe, expect, it, vi } from 'vitest';
import request from '@/utils/request';
import { previewArticle } from './adminArticleApi';

vi.mock('@/utils/request', () => ({ default: { post: vi.fn() } }));

describe('article preview API', () => {
	it('posts only current Markdown to the administrator preview endpoint', async () => {
		vi.mocked(request.post).mockResolvedValue({ contentHtml: '<h1>Preview</h1>' });
		expect(await previewArticle('# Preview')).toEqual({ contentHtml: '<h1>Preview</h1>' });
		expect(request.post).toHaveBeenCalledExactlyOnceWith('admin/articles/preview', {
			contentMd: '# Preview',
		});
	});
});
