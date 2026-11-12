import { describe, expect, it, vi } from 'vitest';
import { useAdminArticleList } from './article/composables/useAdminArticleList';
import { useAdminCategoryList } from './category/composables/useAdminCategoryList';
import { useAdminTagList } from './tag/composables/useAdminTagList';
import { useAdminCommentList } from './comment/composables/useAdminCommentList';
import { useAdminMessageList } from './message/composables/useAdminMessageList';
import { listArticles } from './article/api/adminArticleApi';
import { listCategories } from './category/api/adminCategoryApi';
import { listTags } from './tag/api/adminTagApi';
import { listComments } from './comment/api/adminCommentApi';
import { listMessages } from './message/api/adminMessageApi';

vi.mock('./article/api/adminArticleApi', () => ({ listArticles: vi.fn() }));
vi.mock('./category/api/adminCategoryApi', () => ({ listCategories: vi.fn() }));
vi.mock('./tag/api/adminTagApi', () => ({ listTags: vi.fn() }));
vi.mock('./comment/api/adminCommentApi', () => ({ listComments: vi.fn() }));
vi.mock('./message/api/adminMessageApi', () => ({ listMessages: vi.fn() }));

const page = { records: [], pageNum: 1, pageSize: 10, total: 0, totalPages: 0, hasNext: false };
const cases = [
	{
		name: 'article',
		create: useAdminArticleList,
		request: listArticles,
		method: 'getArticleList',
		result: page,
	},
	{
		name: 'category',
		create: useAdminCategoryList,
		request: listCategories,
		method: 'getCategoryList',
		result: [],
	},
	{ name: 'tag', create: useAdminTagList, request: listTags, method: 'getTagList', result: [] },
	{
		name: 'comment',
		create: useAdminCommentList,
		request: listComments,
		method: 'getCommentList',
		result: page,
	},
	{
		name: 'message',
		create: useAdminMessageList,
		request: listMessages,
		method: 'getMessageList',
		result: page,
	},
] as const;

describe('admin list loading', () => {
	it.each(cases)(
		'$name keeps the mask until the latest request ends and clears it on failure',
		async ({ create, request, method, result }) => {
			let resolveOld!: (value: unknown) => void;
			let rejectLatest!: (reason: Error) => void;
			vi.mocked(request)
				.mockReset()
				.mockReturnValueOnce(
					new Promise((resolve) => {
						resolveOld = resolve;
					}) as never,
				)
				.mockReturnValueOnce(
					new Promise((_, reject) => {
						rejectLatest = reject;
					}) as never,
				);
			const state = create();
			const load = (state as unknown as Record<string, () => Promise<unknown>>)[method]!;
			const old = load();
			const latest = load();
			expect(state.loading.value).toBe(true);
			resolveOld(result);
			await old;
			expect(state.loading.value).toBe(true);
			rejectLatest(new Error('network failed'));
			await latest;
			expect(state.loading.value).toBe(false);
		},
	);
});
