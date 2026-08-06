import { ElMessage, ElMessageBox } from 'element-plus';
import { listComments, moderateComment } from '../api/adminCommentApi';
import {
	COMMENT_MODERATION_ACTION,
	COMMENT_STATUS,
	type AdminCommentFilterForm,
	type AdminCommentItem,
	type AdminCommentListQuery,
	type AdminCommentPageData,
	type AdminCommentPageMeta,
	type AdminCommentPageParams,
	type CommentModerationAction,
} from '../types/adminComment';

export function useAdminCommentList() {
	// 评论列表请求序号，用于避免旧请求响应覆盖新请求结果
	let commentRequestId = 0;

	// 评论列表数据
	const commentList = ref<AdminCommentItem[]>([]);

	// 分页元信息
	const pageMeta = reactive({
		total: 0,
		totalPages: 0,
		hasNext: false,
	} as AdminCommentPageMeta);

	// 分页参数初始值
	const initPageParams: AdminCommentPageParams = {
		pageNum: 1,
		pageSize: 5,
	};

	// 分页参数
	const pageParams = reactive<AdminCommentPageParams>({ ...initPageParams });

	// 条件参数初始值
	const initFilterForm: AdminCommentFilterForm = {
		articleId: undefined,
		userId: undefined,
		status: '',
		type: '',
		createdAtRange: [],
	};

	// 条件参数
	const filterForm = reactive<AdminCommentFilterForm>({ ...initFilterForm });

	// 构造查询参数（合并条件参数和分页参数）
	function buildCommentListQuery(): AdminCommentListQuery {
		const [createdAtFrom, createdAtTo] = filterForm.createdAtRange ?? [];

		return {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			articleId: filterForm.articleId,
			userId: filterForm.userId,
			status: filterForm.status || undefined,
			type: filterForm.type || undefined,
			createdAtFrom,
			createdAtTo,
		};
	}

	// 获取评论列表
	async function getCommentList(page: number = pageParams.pageNum) {
		const currentRequestId = ++commentRequestId;
		pageParams.pageNum = page;
		try {
			const data: AdminCommentPageData = await listComments(buildCommentListQuery());
			if (currentRequestId !== commentRequestId) return;

			commentList.value = data.records;
			pageParams.pageNum = data.pageNum;
			pageParams.pageSize = data.pageSize;
			pageMeta.total = data.total;
			pageMeta.totalPages = data.totalPages;
			pageMeta.hasNext = data.hasNext;
		} catch {
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 重置条件参数
	function resetFilterForm() {
		Object.assign(filterForm, initFilterForm);
		getCommentList(1);
	}

	// 审核通过评论
	async function approveComment(comment: AdminCommentItem) {
		await handleModerateComment(comment, COMMENT_MODERATION_ACTION.APPROVE);
	}

	// 拒绝评论
	async function rejectComment(comment: AdminCommentItem) {
		await handleModerateComment(comment, COMMENT_MODERATION_ACTION.REJECT);
	}

	// 隐藏评论
	async function hideComment(comment: AdminCommentItem) {
		await handleModerateComment(comment, COMMENT_MODERATION_ACTION.HIDE);
	}

	// 删除评论
	async function deleteComment(comment: AdminCommentItem) {
		await handleModerateComment(comment, COMMENT_MODERATION_ACTION.DELETE);
	}

	// 执行评论审核操作；拒绝、隐藏、删除需要填写原因。
	async function handleModerateComment(
		comment: AdminCommentItem,
		action: CommentModerationAction,
	) {
		try {
			const reason = await getModerationReason(action);

			await moderateComment(comment.id, {
				action,
				reason,
			});

			ElMessage.success('评论处理成功');
			await getCommentList();
		} catch (error) {
			if (error === 'cancel' || error === 'close') return;
			// 错误提示已经由 request 响应拦截器统一处理
		}
	}

	// 获取评论处理原因；通过时不需要原因。
	async function getModerationReason(action: CommentModerationAction) {
		if (action === COMMENT_MODERATION_ACTION.APPROVE) {
			return undefined;
		}

		const { value } = await ElMessageBox.prompt('请输入处理原因', getActionLabel(action), {
			confirmButtonText: '确定',
			cancelButtonText: '取消',
			inputType: 'textarea',
			inputPlaceholder: '请输入 1-255 个字符的处理原因',
			inputValidator(value) {
				if (!value.trim()) return '处理原因不能为空';
				if (value.trim().length > 255) return '处理原因长度不能超过 255 个字符';
				return true;
			},
		});

		return value.trim();
	}

	// 获取评论操作展示文案。
	function getActionLabel(action: CommentModerationAction) {
		switch (action) {
			case COMMENT_MODERATION_ACTION.REJECT:
				return '拒绝评论';
			case COMMENT_MODERATION_ACTION.HIDE:
				return '隐藏评论';
			case COMMENT_MODERATION_ACTION.DELETE:
				return '删除评论';
			default:
				return '通过评论';
		}
	}

	// 判断评论是否可以通过。
	function canApprove(comment: AdminCommentItem) {
		return (
			comment.status === COMMENT_STATUS.PENDING ||
			comment.status === COMMENT_STATUS.REJECTED ||
			comment.status === COMMENT_STATUS.HIDDEN
		);
	}

	// 判断评论是否可以拒绝。
	function canReject(comment: AdminCommentItem) {
		return comment.status === COMMENT_STATUS.PENDING;
	}

	// 判断评论是否可以隐藏。
	function canHide(comment: AdminCommentItem) {
		return comment.status === COMMENT_STATUS.APPROVED;
	}

	// 判断评论是否可以删除。
	function canDelete(comment: AdminCommentItem) {
		return comment.status !== COMMENT_STATUS.DELETED;
	}

	return {
		commentList,
		pageMeta,
		pageParams,
		filterForm,
		getCommentList,
		resetFilterForm,
		approveComment,
		rejectComment,
		hideComment,
		deleteComment,
		canApprove,
		canReject,
		canHide,
		canDelete,
	};
}
