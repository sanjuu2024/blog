import request from '@/utils/request';
import type {
	CommentMutationData,
	CommentMutationResponse,
	CommentDeleteData,
	CommentReplyListQuery,
	CommentReplyPageData,
	CommentReplyPageResponse,
	CreateCommentRequest,
	DeleteCommentResponse,
	PublicCommentListQuery,
	PublicCommentPageData,
	PublicCommentPageResponse,
} from '../types/comment';

const COMMENT_API = {
	listComments: (articleId: number) => `articles/${articleId}/comments`,
	createComment: (articleId: number) => `articles/${articleId}/comments`,
	listReplies: (commentId: number) => `comments/${commentId}/replies`,
	deleteComment: (commentId: number) => `comments/${commentId}`,
} as const;

// 获取文章顶层评论分页列表接口
export const listComments = (
	articleId: number,
	params?: PublicCommentListQuery,
): Promise<PublicCommentPageData> => {
	// 🔺注意 params 是 query 参数，所以是作为配置传过去的，request.get() 的第二个参数就是 Axios 请求配置对象，所以把 params 用 {params:params} 传进去。
	// 🔺request.post<后端原始响应, 拦截器最终返回值, 请求体类型>(url, data)
	return request.get<PublicCommentPageResponse, PublicCommentPageData, PublicCommentListQuery>(
		COMMENT_API.listComments(articleId),
		{ params },
	);
};

// 获取顶层评论下的回复接口
export const listReplies = (
	commentId: number,
	params?: CommentReplyListQuery,
): Promise<CommentReplyPageData> => {
	return request.get<CommentReplyPageResponse, CommentReplyPageData, CommentReplyListQuery>(
		COMMENT_API.listReplies(commentId),
		{ params },
	);
};

// 发表评论或回复接口
export const createComment = (
	articleId: number,
	data: CreateCommentRequest,
): Promise<CommentMutationData> => {
	return request.post<CommentMutationResponse, CommentMutationData, CreateCommentRequest>(
		COMMENT_API.createComment(articleId),
		data,
	);
};

// 删除自己的评论接口
export const deleteComment = (commentId: number): Promise<CommentDeleteData> => {
	return request.delete<DeleteCommentResponse, CommentDeleteData, null>(
		COMMENT_API.deleteComment(commentId),
	);
};
