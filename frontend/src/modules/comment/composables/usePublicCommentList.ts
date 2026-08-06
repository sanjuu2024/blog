import { ElMessage, ElMessageBox } from 'element-plus';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import { createComment, deleteComment, listComments, listReplies } from '../api/commentApi';
import {
	COMMENT_STATUS,
	type CommentDeleteData,
	type CommentMutationData,
	type CommentReplyItem,
	type CommentReplyState,
	type CommentAuthor,
	type PublicCommentItem,
	type PublicCommentPageData,
} from '../types/comment';

const COMMENT_PAGE_SIZE = 10;
const REPLY_FIRST_PAGE_SIZE = 5;
const REPLY_NEXT_PAGE_SIZE = 10;

export function usePublicCommentList() {
	// 顶层评论请求序号，用于避免旧请求响应覆盖新请求结果
	let commentRequestId = 0;

	// 顶层评论列表
	const commentList = ref<PublicCommentItem[]>([]);

	// 顶层评论分页参数
	const pageParams = reactive({
		pageNum: 1,
		pageSize: COMMENT_PAGE_SIZE,
		hasNext: false,
	});

	// 顶层评论是否正在加载
	const loading = ref(false);

	// 每条顶层评论自己的回复状态
	const replyStateMap = reactive<Record<number, CommentReplyState>>({});

	// 获取指定顶层评论的回复状态
	function getReplyState(commentId: number) {
		if (!replyStateMap[commentId]) {
			replyStateMap[commentId] = {
				records: [],
				nextCursor: null,
				hasNext: false,
				loading: false,
				loaded: false,
				expanded: false,
			};
		}

		return replyStateMap[commentId];
	}

	// 获取顶层评论列表
	async function getCommentList(articleId: number, pageNum: number = 1) {
		const currentRequestId = ++commentRequestId;
		loading.value = true;
		pageParams.pageNum = pageNum;

		try {
			const data: PublicCommentPageData = await listComments(articleId, {
				pageNum,
				pageSize: pageParams.pageSize,
			});

			if (currentRequestId !== commentRequestId) return;

			commentList.value =
				pageNum === 1 ? data.records : [...commentList.value, ...data.records];
			pageParams.pageNum = data.pageNum;
			pageParams.hasNext = data.hasNext;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		} finally {
			if (currentRequestId === commentRequestId) {
				loading.value = false;
			}
		}
	}

	// 重置顶层评论列表并重新获取第一页
	async function resetCommentList(articleId: number) {
		commentList.value = [];
		Object.keys(replyStateMap).forEach((key) => {
			delete replyStateMap[Number(key)];
		});
		pageParams.pageNum = 1;
		pageParams.hasNext = false;

		await getCommentList(articleId, 1);
	}

	// 加载更多顶层评论
	async function loadMoreComments(articleId: number) {
		if (loading.value || !pageParams.hasNext) return;

		await getCommentList(articleId, pageParams.pageNum + 1);
	}

	// 获取指定顶层评论下的回复
	async function getReplyList(commentId: number, reset = false) {
		const state = getReplyState(commentId);
		if (state.loading) return;

		state.loading = true;
		state.expanded = true;

		try {
			const data = await listReplies(commentId, {
				limit: reset ? REPLY_FIRST_PAGE_SIZE : REPLY_NEXT_PAGE_SIZE,
				cursor: reset ? null : state.nextCursor,
			});

			state.records = reset
				? dedupeReplies(data.records)
				: mergeUniqueReplies(state.records, data.records);
			state.nextCursor = data.nextCursor;
			state.hasNext = data.hasNext;
			state.loaded = true;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		} finally {
			state.loading = false;
		}
	}

	// 收起指定顶层评论下的回复
	function collapseReplies(commentId: number) {
		getReplyState(commentId).expanded = false;
	}

	// 发表评论
	async function submitTopLevelComment(articleId: number, content: string) {
		const data = await createComment(articleId, { content });
		commentList.value = [buildTopLevelComment(data), ...commentList.value];
		await getCommentList(articleId, 1);

		ElMessage.success(
			data.status === COMMENT_STATUS.APPROVED
				? '评论发布成功'
				: '评论已提交，审核通过后会公开展示',
		);

		return data;
	}

	// 回复评论
	async function submitReply(
		articleId: number,
		rootComment: PublicCommentItem,
		parentId: number,
		replyToUser: CommentAuthor | null,
		content: string,
	) {
		const data = await createComment(articleId, {
			content,
			parentId,
		});

		const state = getReplyState(rootComment.id);
		if (!state.loaded) {
			await getReplyList(rootComment.id, true);
		}

		const reply = buildReply(data, rootComment.id, replyToUser);
		state.expanded = true;
		state.records = insertReplyAfterTarget(
			state.records,
			reply,
			reply.parentId,
			rootComment.id,
		);
		rootComment.hasVisibleReplies = true;
		if (data.status === COMMENT_STATUS.APPROVED) {
			rootComment.replyCount += 1;
		}

		ElMessage.success(
			data.status === COMMENT_STATUS.APPROVED
				? '回复发布成功'
				: '回复已提交，审核通过前仅自己可见',
		);

		return data;
	}

	// 删除自己的评论
	async function deleteOwnComment(
		comment: PublicCommentItem | CommentReplyItem,
	): Promise<CommentDeleteData> {
		await ElMessageBox.confirm('确认删除这条评论吗？删除后其子回复也会一并删除。', '删除评论', {
			confirmButtonText: '确定',
			cancelButtonText: '取消',
			type: 'warning',
		});

		const data: CommentDeleteData = await deleteComment(comment.id);
		ElMessage.success('评论已删除');
		return data;
	}

	// 删除顶层评论后，同步本地列表
	async function removeTopLevelComment(articleId: number, comment: PublicCommentItem) {
		commentList.value = commentList.value.filter((item) => item.id !== comment.id);
		delete replyStateMap[comment.id];
		await getCommentList(articleId, 1);
	}

	// 删除回复后，同步本地回复列表
	async function removeReply(
		rootComment: PublicCommentItem,
		reply: CommentReplyItem,
		deletedApprovedCount: number,
	) {
		const state = getReplyState(rootComment.id);
		const deletedIds = collectDeletedReplyIds(state.records, reply.id);

		state.records = state.records.filter((item) => !deletedIds.includes(item.id));
		rootComment.replyCount = Math.max(0, rootComment.replyCount - deletedApprovedCount);
		rootComment.hasVisibleReplies =
			rootComment.replyCount > 0 || state.records.length > 0 || state.hasNext;

		// 服务端可能同时删除尚未加载的子孙回复，重新获取第一页以同步游标和剩余数据
		await getReplyList(rootComment.id, true);
		rootComment.hasVisibleReplies =
			rootComment.replyCount > 0 || state.records.length > 0 || state.hasNext;
	}

	// 查询被删除回复及其所有子回复 ID
	function collectDeletedReplyIds(replies: CommentReplyItem[], replyId: number) {
		const deletedIds = [replyId];

		replies.forEach((reply) => {
			if (deletedIds.includes(reply.parentId)) {
				deletedIds.push(reply.id);
			}
		});

		return deletedIds;
	}

	// 本地插入新回复时，优先放在它回复的目标评论下方
	function insertReplyAfterTarget(
		replies: CommentReplyItem[],
		reply: CommentReplyItem,
		parentId: number,
		rootCommentId: number,
	) {
		if (replies.some((item) => item.id === reply.id)) return replies;

		if (parentId === rootCommentId) return [reply, ...replies];

		const targetIndex = replies.findIndex((item) => item.id === parentId);
		if (targetIndex === -1) return [...replies, reply];

		return [...replies.slice(0, targetIndex + 1), reply, ...replies.slice(targetIndex + 1)];
	}

	// 合并回复列表，并按 id 去重
	function mergeUniqueReplies(
		existingReplies: CommentReplyItem[],
		newReplies: CommentReplyItem[],
	) {
		const mergedReplies = [...existingReplies];
		const existingIds = new Set(existingReplies.map((reply) => reply.id));

		newReplies.forEach((reply) => {
			if (existingIds.has(reply.id)) return;
			existingIds.add(reply.id);
			mergedReplies.push(reply);
		});

		return mergedReplies;
	}

	// 对单批回复去重
	function dedupeReplies(replies: CommentReplyItem[]) {
		return mergeUniqueReplies([], replies);
	}

	// 把创建结果转为顶层评论列表项
	function buildTopLevelComment(data: CommentMutationData): PublicCommentItem {
		return {
			id: data.id,
			articleId: data.articleId,
			content: data.content,
			status: data.status,
			moderationReason: data.moderationReason,
			author: data.author,
			replyCount: 0,
			hasVisibleReplies: false,
			isMine: true,
			createdAt: data.createdAt,
		};
	}

	// 把创建结果转为回复列表项
	function buildReply(
		data: CommentMutationData,
		rootId: number,
		replyToUser: CommentAuthor | null,
	): CommentReplyItem {
		return {
			...buildTopLevelComment(data),
			parentId: data.parentId ?? rootId,
			rootId,
			replyToUser,
		};
	}

	return {
		commentList,
		pageParams,
		loading,
		getReplyState,
		getCommentList,
		resetCommentList,
		loadMoreComments,
		getReplyList,
		collapseReplies,
		submitTopLevelComment,
		submitReply,
		deleteOwnComment,
		removeTopLevelComment,
		removeReply,
	};
}
