import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createComment, listComments, listReplies } from '../api/commentApi';
import {
	COMMENT_STATUS,
	type CommentAuthor,
	type CommentMutationData,
	type CommentReplyItem,
	type PublicCommentItem,
	type PublicCommentPageData,
} from '../types/comment';
import { usePublicCommentList } from './usePublicCommentList';

vi.mock('../api/commentApi', () => ({
	createComment: vi.fn(),
	deleteComment: vi.fn(),
	listComments: vi.fn(),
	listReplies: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: {
		success: vi.fn(),
	},
	ElMessageBox: {
		confirm: vi.fn(),
	},
}));

const AUTHOR: CommentAuthor = {
	id: 10001,
	username: 'alice',
	nickname: 'Alice',
	avatarUrl: '',
};

function topLevelComment(overrides: Partial<PublicCommentItem> = {}): PublicCommentItem {
	return {
		id: 50001,
		articleId: 40001,
		content: '顶层评论',
		status: COMMENT_STATUS.APPROVED,
		moderationReason: null,
		author: AUTHOR,
		replyCount: 0,
		hasVisibleReplies: false,
		isMine: false,
		createdAt: '2026-05-08T09:20:00+08:00',
		...overrides,
	};
}

function reply(id: number, parentId: number, rootId: number = 50001): CommentReplyItem {
	return {
		...topLevelComment({ id, content: `回复 ${id}` }),
		parentId,
		rootId,
		replyToUser: AUTHOR,
	};
}

function commentPage(overrides: Partial<PublicCommentPageData> = {}): PublicCommentPageData {
	return {
		total: 0,
		totalPages: 0,
		pageNum: 1,
		pageSize: 10,
		hasNext: false,
		records: [],
		...overrides,
	};
}

describe('usePublicCommentList', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('loads 5 replies first and 10 replies on subsequent requests', async () => {
		vi.mocked(listReplies)
			.mockResolvedValueOnce({
				records: [reply(50002, 50001)],
				nextCursor: 'next-cursor',
				hasNext: true,
			})
			.mockResolvedValueOnce({
				records: [reply(50003, 50001)],
				nextCursor: null,
				hasNext: false,
			});

		const { getReplyList } = usePublicCommentList();

		await getReplyList(50001, true);
		await getReplyList(50001);

		expect(listReplies).toHaveBeenNthCalledWith(1, 50001, {
			limit: 5,
			cursor: null,
		});
		expect(listReplies).toHaveBeenNthCalledWith(2, 50001, {
			limit: 10,
			cursor: 'next-cursor',
		});
	});

	it('reloads the first page after creating a top-level comment', async () => {
		const createdComment: CommentMutationData = {
			id: 50002,
			articleId: 40001,
			parentId: null,
			rootId: null,
			content: '新评论',
			status: COMMENT_STATUS.PENDING,
			moderationReason: null,
			author: AUTHOR,
			createdAt: '2026-05-08T09:30:00+08:00',
		};
		const serverComment = topLevelComment({
			id: createdComment.id,
			content: createdComment.content,
			status: COMMENT_STATUS.PENDING,
			isMine: true,
		});
		vi.mocked(createComment).mockResolvedValue(createdComment);
		vi.mocked(listComments).mockResolvedValue(
			commentPage({ total: 1, totalPages: 1, records: [serverComment] }),
		);

		const { commentList, submitTopLevelComment } = usePublicCommentList();
		await submitTopLevelComment(createdComment.articleId, createdComment.content);

		expect(listComments).toHaveBeenCalledWith(createdComment.articleId, {
			pageNum: 1,
			pageSize: 10,
		});
		expect(commentList.value).toEqual([serverComment]);
	});

	it('reloads the first page after removing a top-level comment', async () => {
		const deletedComment = topLevelComment({ id: 50001 });
		const remainingComment = topLevelComment({ id: 50002 });
		vi.mocked(listComments).mockResolvedValue(
			commentPage({ total: 1, totalPages: 1, records: [remainingComment] }),
		);

		const { commentList, removeTopLevelComment } = usePublicCommentList();
		commentList.value = [deletedComment, remainingComment];
		await removeTopLevelComment(deletedComment.articleId, deletedComment);

		expect(listComments).toHaveBeenCalledWith(deletedComment.articleId, {
			pageNum: 1,
			pageSize: 10,
		});
		expect(commentList.value).toEqual([remainingComment]);
	});

	it('inserts a pending reply after its target without increasing approved reply count', async () => {
		const rootComment = topLevelComment({ replyCount: 2 });
		const targetReply = reply(50002, rootComment.id);
		const otherReply = reply(50003, rootComment.id);
		const createdReply: CommentMutationData = {
			id: 50004,
			articleId: rootComment.articleId,
			parentId: targetReply.id,
			rootId: rootComment.id,
			content: '回复目标评论',
			status: COMMENT_STATUS.PENDING,
			moderationReason: null,
			author: AUTHOR,
			createdAt: '2026-05-08T09:30:00+08:00',
		};
		vi.mocked(createComment).mockResolvedValue(createdReply);

		const { getReplyState, submitReply } = usePublicCommentList();
		const state = getReplyState(rootComment.id);
		state.records = [targetReply, otherReply];
		state.loaded = true;

		await submitReply(
			rootComment.articleId,
			rootComment,
			targetReply.id,
			targetReply.author,
			createdReply.content,
		);

		expect(getReplyState(rootComment.id).records.map((item) => item.id)).toEqual([
			targetReply.id,
			createdReply.id,
			otherReply.id,
		]);
		expect(rootComment.replyCount).toBe(2);
		expect(rootComment.hasVisibleReplies).toBe(true);
	});

	it('loads existing replies before inserting a reply when the list was not expanded', async () => {
		const rootComment = topLevelComment({ replyCount: 1, hasVisibleReplies: true });
		const existingReply = reply(50002, rootComment.id);
		const createdReply: CommentMutationData = {
			id: 50003,
			articleId: rootComment.articleId,
			parentId: rootComment.id,
			rootId: rootComment.id,
			content: '直接回复顶层评论',
			status: COMMENT_STATUS.PENDING,
			moderationReason: null,
			author: AUTHOR,
			createdAt: '2026-05-08T09:30:00+08:00',
		};
		vi.mocked(createComment).mockResolvedValue(createdReply);
		vi.mocked(listReplies).mockResolvedValue({
			records: [existingReply],
			nextCursor: null,
			hasNext: false,
		});

		const { getReplyState, submitReply } = usePublicCommentList();
		await submitReply(
			rootComment.articleId,
			rootComment,
			rootComment.id,
			rootComment.author,
			createdReply.content,
		);

		expect(listReplies).toHaveBeenCalledWith(rootComment.id, {
			limit: 5,
			cursor: null,
		});
		expect(getReplyState(rootComment.id).loaded).toBe(true);
		expect(getReplyState(rootComment.id).records.map((item) => item.id)).toEqual([
			createdReply.id,
			existingReply.id,
		]);
	});

	it('does not duplicate a locally inserted reply when loading more', async () => {
		const localReply = reply(50004, 50002);
		const nextReply = reply(50005, 50001);
		vi.mocked(listReplies).mockResolvedValue({
			records: [localReply, nextReply],
			nextCursor: null,
			hasNext: false,
		});

		const { getReplyList, getReplyState } = usePublicCommentList();
		const state = getReplyState(50001);
		state.records = [localReply];
		state.nextCursor = 'next-cursor';
		state.loaded = true;

		await getReplyList(50001);

		expect(state.records.map((item) => item.id)).toEqual([localReply.id, nextReply.id]);
	});

	it('removes loaded descendants and reloads the first reply page', async () => {
		const rootComment = topLevelComment({ replyCount: 3 });
		const parentReply = reply(50002, rootComment.id);
		const childReply = reply(50003, parentReply.id);
		const nestedReply = reply(50004, childReply.id);
		nestedReply.status = COMMENT_STATUS.PENDING;
		const siblingReply = reply(50005, rootComment.id);

		const { getReplyState, removeReply } = usePublicCommentList();
		const state = getReplyState(rootComment.id);
		state.records = [parentReply, childReply, nestedReply, siblingReply];
		state.nextCursor = 'stale-cursor';
		state.hasNext = true;
		state.loaded = true;
		vi.mocked(listReplies).mockResolvedValue({
			records: [siblingReply],
			nextCursor: null,
			hasNext: false,
		});

		await removeReply(rootComment, parentReply, 2);

		expect(listReplies).toHaveBeenCalledWith(rootComment.id, {
			limit: 5,
			cursor: null,
		});
		expect(getReplyState(rootComment.id).records).toEqual([siblingReply]);
		expect(getReplyState(rootComment.id).nextCursor).toBeNull();
		expect(getReplyState(rootComment.id).hasNext).toBe(false);
		expect(rootComment.replyCount).toBe(1);
		expect(rootComment.hasVisibleReplies).toBe(true);
	});
});
