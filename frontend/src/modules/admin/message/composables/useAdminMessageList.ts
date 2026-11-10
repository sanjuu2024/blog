import { ElMessage, ElMessageBox } from 'element-plus';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import {
	listMessages,
	moderateMessage,
	replyMessage,
	approveMessages,
} from '../api/adminMessageApi';
import {
	MESSAGE_MODERATION_ACTION,
	MESSAGE_STATUS,
	type AdminMessageFilterForm,
	type AdminMessageItem,
	type AdminMessageListQuery,
	type AdminMessagePageData,
	type MessageModerationAction,
} from '../types/adminMessage';

export function useAdminMessageList() {
	let requestId = 0;
	const messageList = ref<AdminMessageItem[]>([]);
	const selectedMessageIds = ref<number[]>([]);
	// 审核、回复和批量通过共用提交状态，避免快速点击产生重复写请求。
	const submitting = ref(false);
	const pageMeta = reactive({ total: 0, totalPages: 0, hasNext: false });
	const pageParams = reactive({ pageNum: 1, pageSize: 10 });
	const filterForm = reactive<AdminMessageFilterForm>({
		messageId: undefined,
		userId: undefined,
		guestNickname: '',
		guestEmail: '',
		content: '',
		status: '',
		type: '',
		createdAtRange: [],
	});

	function buildQuery(): AdminMessageListQuery {
		const [createdAtFrom, createdAtTo] = filterForm.createdAtRange ?? [];
		return {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			messageId: filterForm.messageId,
			userId: filterForm.userId,
			guestNickname: filterForm.guestNickname?.trim() || undefined,
			guestEmail: filterForm.guestEmail?.trim() || undefined,
			content: filterForm.content?.trim() || undefined,
			status: filterForm.status || undefined,
			type: filterForm.type || undefined,
			createdAtFrom,
			createdAtTo,
		};
	}

	async function getMessageList(page = pageParams.pageNum) {
		const currentRequestId = ++requestId;
		pageParams.pageNum = page;
		try {
			const data: AdminMessagePageData = await listMessages(buildQuery());
			if (currentRequestId !== requestId) return;
			messageList.value = data.records;
			selectedMessageIds.value = [];
			pageParams.pageNum = data.pageNum;
			pageParams.pageSize = data.pageSize;
			pageMeta.total = data.total;
			pageMeta.totalPages = data.totalPages;
			pageMeta.hasNext = data.hasNext;
		} catch {
			// 请求错误由统一响应拦截器提示。
		}
	}

	function resetFilterForm() {
		Object.assign(filterForm, {
			messageId: undefined,
			userId: undefined,
			guestNickname: '',
			guestEmail: '',
			content: '',
			status: '',
			type: '',
			createdAtRange: [],
		});
		getMessageList(1);
	}

	async function handleModerate(item: AdminMessageItem, action: MessageModerationAction) {
		if (submitting.value) return false;
		submitting.value = true;
		try {
			const reason = await getModerationReason(action);
			await moderateMessage(item.id, { action, reason });
			ElMessage.success('留言处理成功');
			await getMessageList();
			return true;
		} catch (error) {
			if (error === 'cancel' || error === 'close') return false;
			// 请求错误由统一响应拦截器提示。
			return false;
		} finally {
			submitting.value = false;
		}
	}

	async function getModerationReason(action: MessageModerationAction) {
		if (action === MESSAGE_MODERATION_ACTION.APPROVE) return undefined;
		const { value } = await ElMessageBox.prompt('请输入处理原因', getActionLabel(action), {
			confirmButtonText: '确定',
			cancelButtonText: '取消',
			inputType: 'textarea',
			inputValidator(value) {
				if (!value.trim()) return '处理原因不能为空';
				if (value.trim().length > 255) return '处理原因长度不能超过 255 个字符';
				return true;
			},
		});
		return value.trim();
	}

	function getActionLabel(action: MessageModerationAction) {
		return {
			[MESSAGE_MODERATION_ACTION.REJECT]: '拒绝留言',
			[MESSAGE_MODERATION_ACTION.HIDE]: '隐藏留言',
			[MESSAGE_MODERATION_ACTION.DELETE]: '删除留言',
			[MESSAGE_MODERATION_ACTION.APPROVE]: '通过留言',
		}[action];
	}

	async function reply(item: AdminMessageItem, content: string) {
		if (submitting.value) return false;
		submitting.value = true;
		try {
			await replyMessage(item.id, { content });
			ElMessage.success('回复成功');
			await getMessageList();
			return true;
		} catch {
			// 请求错误由统一响应拦截器提示。
			return false;
		} finally {
			submitting.value = false;
		}
	}

	async function approveSelected() {
		if (!selectedMessageIds.value.length || submitting.value) return false;
		submitting.value = true;
		try {
			await approveMessages({ messageIds: selectedMessageIds.value });
			ElMessage.success('批量通过成功');
			await getMessageList();
			return true;
		} catch {
			// 请求错误由统一响应拦截器提示。
			return false;
		} finally {
			submitting.value = false;
		}
	}

	function canApprove(item: AdminMessageItem) {
		return (
			item.status === MESSAGE_STATUS.PENDING ||
			item.status === MESSAGE_STATUS.REJECTED ||
			item.status === MESSAGE_STATUS.HIDDEN
		);
	}

	function canReply(item: AdminMessageItem) {
		return item.parentId === null && item.status === MESSAGE_STATUS.APPROVED;
	}

	function canReject(item: AdminMessageItem) {
		return item.status === MESSAGE_STATUS.PENDING;
	}
	function canHide(item: AdminMessageItem) {
		return item.status === MESSAGE_STATUS.APPROVED;
	}
	function canDelete(item: AdminMessageItem) {
		return item.status !== MESSAGE_STATUS.DELETED;
	}

	return {
		messageList,
		selectedMessageIds,
		submitting,
		pageMeta,
		pageParams,
		filterForm,
		getMessageList,
		resetFilterForm,
		handleModerate,
		reply,
		approveSelected,
		canApprove,
		canReply,
		canReject,
		canHide,
		canDelete,
	};
}
