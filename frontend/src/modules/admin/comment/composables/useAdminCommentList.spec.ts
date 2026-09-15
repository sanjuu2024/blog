import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listComments, moderateComment } from '../api/adminCommentApi';
import {
	COMMENT_STATUS,
	COMMENT_TYPE,
	type AdminCommentItem,
	type AdminCommentPageData,
} from '../types/adminComment';
import { useAdminCommentList } from './useAdminCommentList';

vi.mock('../api/adminCommentApi', () => ({
	listComments: vi.fn(),
	moderateComment: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		success: vi.fn(),
	},
	ElMessageBox: {
		prompt: vi.fn(),
	},
}));

function comment(overrides: Partial<AdminCommentItem> = {}): AdminCommentItem {
	return {
		id: 50001,
		articleId: 40001,
		parentId: null,
		rootId: null,
		content: '评论内容',
		status: COMMENT_STATUS.PENDING,
		moderationReason: null,
		author: null,
		reviewedBy: null,
		reviewedAt: null,
		deletedBy: null,
		deletedAt: null,
		createdAt: '2026-05-08T09:20:00+08:00',
		article: null,
		type: COMMENT_TYPE.TOP_LEVEL,
		...overrides,
	};
}

function commentPage(overrides: Partial<AdminCommentPageData> = {}): AdminCommentPageData {
	return {
		total: 0,
		totalPages: 0,
		pageNum: 1,
		pageSize: 5,
		hasNext: false,
		records: [],
		...overrides,
	};
}

describe('useAdminCommentList', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('builds the admin comment query from filters', async () => {
		vi.mocked(listComments).mockResolvedValue(commentPage());
		const { filterForm, getCommentList } = useAdminCommentList();
		filterForm.articleId = 40001;
		filterForm.userId = 10001;
		filterForm.status = COMMENT_STATUS.PENDING;
		filterForm.type = COMMENT_TYPE.REPLY;
		filterForm.createdAtRange = ['2026-05-01T00:00:00+08:00', '2026-05-31T23:59:59+08:00'];

		await getCommentList(2);

		expect(listComments).toHaveBeenCalledWith({
			pageNum: 2,
			pageSize: 7,
			articleId: 40001,
			userId: 10001,
			status: COMMENT_STATUS.PENDING,
			type: COMMENT_TYPE.REPLY,
			createdAtFrom: '2026-05-01T00:00:00+08:00',
			createdAtTo: '2026-05-31T23:59:59+08:00',
		});
	});

	it('ignores an older response when a newer request has finished', async () => {
		let resolveFirst!: (data: AdminCommentPageData) => void;
		let resolveSecond!: (data: AdminCommentPageData) => void;
		const firstRequest = new Promise<AdminCommentPageData>((resolve) => {
			resolveFirst = resolve;
		});
		const secondRequest = new Promise<AdminCommentPageData>((resolve) => {
			resolveSecond = resolve;
		});
		vi.mocked(listComments)
			.mockReturnValueOnce(firstRequest)
			.mockReturnValueOnce(secondRequest);

		const { commentList, getCommentList } = useAdminCommentList();
		const first = getCommentList(1);
		const second = getCommentList(2);

		resolveSecond(commentPage({ pageNum: 2, records: [comment({ id: 50002 })] }));
		await second;
		resolveFirst(commentPage({ records: [comment({ id: 50001 })] }));
		await first;

		expect(commentList.value.map((item) => item.id)).toEqual([50002]);
	});

	it('refreshes the current page after moderation succeeds', async () => {
		const target = comment();
		vi.mocked(moderateComment).mockResolvedValue({
			...target,
			status: COMMENT_STATUS.APPROVED,
		});
		vi.mocked(listComments).mockResolvedValue(commentPage({ records: [target] }));

		const { approveComment } = useAdminCommentList();
		await approveComment(target);

		expect(moderateComment).toHaveBeenCalledWith(target.id, {
			action: 'APPROVE',
			reason: undefined,
		});
		expect(listComments).toHaveBeenCalledTimes(1);
	});
});
