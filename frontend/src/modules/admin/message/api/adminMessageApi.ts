import request from '@/utils/request';
import type {
	AdminMessageListQuery,
	AdminMessagePageData,
	AdminMessagePageResponse,
	CreateMessageReplyRequest,
	MessageBatchApprovalRequest,
	MessageModerationRequest,
	MessageMutationData,
	MessageMutationResponse,
} from '../types/adminMessage';

const ADMIN_MESSAGE_API = {
	listMessages: 'admin/messages',
	moderateMessage: (messageId: number) => `admin/messages/${messageId}/moderation`,
	replyMessage: (messageId: number) => `admin/messages/${messageId}/replies`,
	approveMessages: 'admin/messages/batch-approval',
} as const;

// 获取后台留言分页列表接口
export const listMessages = (params?: AdminMessageListQuery): Promise<AdminMessagePageData> => {
	return request.get<AdminMessagePageResponse, AdminMessagePageData, AdminMessageListQuery>(
		ADMIN_MESSAGE_API.listMessages,
		{ params },
	);
};

// 审核、隐藏或删除留言接口
export const moderateMessage = (
	messageId: number,
	data: MessageModerationRequest,
): Promise<MessageMutationData> => {
	return request.patch<MessageMutationResponse, MessageMutationData, MessageModerationRequest>(
		ADMIN_MESSAGE_API.moderateMessage(messageId),
		data,
	);
};

// 管理员回复留言接口
export const replyMessage = (
	messageId: number,
	data: CreateMessageReplyRequest,
): Promise<MessageMutationData> => {
	return request.post<MessageMutationResponse, MessageMutationData, CreateMessageReplyRequest>(
		ADMIN_MESSAGE_API.replyMessage(messageId),
		data,
	);
};

// 批量通过待审核顶层留言接口
export const approveMessages = (data: MessageBatchApprovalRequest): Promise<null> => {
	return request.patch<{ data: null }, null, MessageBatchApprovalRequest>(
		ADMIN_MESSAGE_API.approveMessages,
		data,
	);
};
