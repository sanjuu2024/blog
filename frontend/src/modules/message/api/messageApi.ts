import request from '@/utils/request';
import type {
	CreateMessageRequest,
	MessageMutationData,
	MessageMutationResponse,
	MessagePageQuery,
	MessageUnsubscribeRequest,
	MessageUnsubscribeResponse,
	PublicMessagePageData,
	PublicMessagePageResponse,
} from '../types/message';

const MESSAGE_API = {
	listMessages: 'messages',
	createMessage: 'messages',
	deleteMessage: (messageId: number) => `messages/${messageId}`,
	unsubscribe: 'messages/notifications/unsubscribe',
} as const;

// 获取公开留言分页列表接口
export const listMessages = (params?: MessagePageQuery): Promise<PublicMessagePageData> => {
	return request.get<PublicMessagePageResponse, PublicMessagePageData, MessagePageQuery>(
		MESSAGE_API.listMessages,
		{ params },
	);
};

// 发表留言接口
export const createMessage = (data: CreateMessageRequest): Promise<MessageMutationData> => {
	return request.post<MessageMutationResponse, MessageMutationData, CreateMessageRequest>(
		MESSAGE_API.createMessage,
		data,
	);
};

// 删除自己的留言接口
export const deleteMessage = (messageId: number): Promise<null> => {
	return request.delete<MessageUnsubscribeResponse, null, null>(
		MESSAGE_API.deleteMessage(messageId),
	);
};

// 退订留言回复通知接口
export const unsubscribeMessage = (data: MessageUnsubscribeRequest): Promise<null> => {
	return request.post<MessageUnsubscribeResponse, null, MessageUnsubscribeRequest>(
		MESSAGE_API.unsubscribe,
		data,
	);
};
