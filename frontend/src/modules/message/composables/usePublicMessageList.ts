import { ElMessage, ElMessageBox } from 'element-plus';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import { createMessage, deleteMessage, listMessages } from '../api/messageApi';
import {
	MESSAGE_STATUS,
	type CreateMessageRequest,
	type MessageMutationData,
	type PublicMessageItem,
	type PublicMessagePageData,
} from '../types/message';

const MESSAGE_PAGE_SIZE = 10;

export function usePublicMessageList() {
	const messageList = ref<PublicMessageItem[]>([]);
	const loading = ref(false);
	const pageParams = reactive({
		pageNum: 1,
		pageSize: MESSAGE_PAGE_SIZE,
		hasNext: false,
	});
	let requestId = 0;

	// 获取公开留言列表，第二页开始追加，不重新加载整个页面。
	async function getMessageList(pageNum = 1) {
		const currentRequestId = ++requestId;
		loading.value = true;
		try {
			const data: PublicMessagePageData = await listMessages({
				pageNum,
				pageSize: pageParams.pageSize,
			});
			if (currentRequestId !== requestId) return;
			messageList.value =
				pageNum === 1 ? data.records : mergeUniqueMessages(messageList.value, data.records);
			pageParams.pageNum = data.pageNum;
			pageParams.hasNext = data.hasNext;
		} catch {
			// 请求错误由统一响应拦截器提示。
		} finally {
			if (currentRequestId === requestId) loading.value = false;
		}
	}

	async function loadMoreMessages() {
		if (loading.value || !pageParams.hasNext) return;
		await getMessageList(pageParams.pageNum + 1);
	}

	// 游客提交后不把待审核留言插入公开列表；登录用户可以立即看到自己的返回值。
	async function submitMessage(form: CreateMessageRequest) {
		const data = await createMessage(form);
		if (data.isMine && data.status !== MESSAGE_STATUS.DELETED) {
			messageList.value = [buildMessage(data), ...messageList.value];
		}
		ElMessage.success(
			data.status === MESSAGE_STATUS.APPROVED
				? '留言发布成功'
				: '留言已提交，审核通过后会公开展示',
		);
		return data;
	}

	async function removeMessage(message: PublicMessageItem) {
		try {
			await ElMessageBox.confirm('确认删除这条留言吗？管理员回复也会一并删除。', '删除留言', {
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning',
			});
			await deleteMessage(message.id);
			messageList.value = messageList.value.filter((item) => item.id !== message.id);
			await getMessageList(1);
			ElMessage.success('留言已删除');
		} catch (error) {
			if (error === 'cancel' || error === 'close') return;
		}
	}

	// 本地插入留言后，offset 分页的下一页可能再次返回该留言，这里按 ID 合并去重。
	function mergeUniqueMessages(
		existingMessages: PublicMessageItem[],
		newMessages: PublicMessageItem[],
	) {
		const mergedMessages = [...existingMessages];
		const existingIds = new Set(existingMessages.map((message) => message.id));

		newMessages.forEach((message) => {
			if (existingIds.has(message.id)) return;
			existingIds.add(message.id);
			mergedMessages.push(message);
		});

		return mergedMessages;
	}

	function buildMessage(data: MessageMutationData): PublicMessageItem {
		return {
			id: data.id,
			nickname: data.nickname,
			content: data.content,
			status: data.status,
			moderationReason: data.moderationReason,
			author: data.author,
			isMine: data.isMine,
			replies: data.replies,
			createdAt: data.createdAt,
		};
	}

	return {
		messageList,
		loading,
		pageParams,
		getMessageList,
		loadMoreMessages,
		submitMessage,
		removeMessage,
	};
}
