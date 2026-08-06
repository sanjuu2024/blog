import request from '@/utils/request';
import type {
	AdminCommentListQuery,
	AdminCommentPageData,
	AdminCommentPageResponse,
	CommentModerationRequest,
	CommentMutationData,
	CommentMutationResponse,
} from '../types/adminComment';

const ADMIN_COMMENT_API = {
	listComments: 'admin/comments',
	moderateComment: (commentId: number) => `admin/comments/${commentId}/moderation`,
} as const;

// 获取后台评论分页列表接口
export const listComments = (params?: AdminCommentListQuery): Promise<AdminCommentPageData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<AdminCommentPageResponse, AdminCommentPageData, AdminCommentListQuery>(
		ADMIN_COMMENT_API.listComments,
		{ params },
	);
};

// 审核、隐藏或删除评论接口
export const moderateComment = (
	commentId: number,
	data: CommentModerationRequest,
): Promise<CommentMutationData> => {
	return request.patch<CommentMutationResponse, CommentMutationData, CommentModerationRequest>(
		ADMIN_COMMENT_API.moderateComment(commentId),
		data,
	);
};
