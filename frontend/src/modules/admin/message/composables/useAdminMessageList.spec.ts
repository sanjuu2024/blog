import { beforeEach, describe, expect, it, vi } from 'vitest';
import { approveMessages, listMessages, replyMessage } from '../api/adminMessageApi';
import {
	MESSAGE_STATUS,
	MESSAGE_TYPE,
	type AdminMessageItem,
	type AdminMessagePageData,
} from '../types/adminMessage';
import { useAdminMessageList } from './useAdminMessageList';

vi.mock('../api/adminMessageApi', () => ({
	approveMessages: vi.fn(),
	listMessages: vi.fn(),
	moderateMessage: vi.fn(),
	replyMessage: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: { success: vi.fn() },
	ElMessageBox: { prompt: vi.fn() },
}));

function page(): AdminMessagePageData {
	return {
		total: 0,
		totalPages: 0,
		pageNum: 1,
		pageSize: 10,
		hasNext: false,
		records: [],
	};
}

function message(
	status: AdminMessageItem['status'],
	parentId: number | null = null,
): AdminMessageItem {
	return {
		id: 90001,
		userId: null,
		parentId,
		nickname: '访客',
		email: '',
		content: '留言内容',
		status,
		type: parentId === null ? MESSAGE_TYPE.TOP_LEVEL : MESSAGE_TYPE.REPLY,
		notifyOnReply: false,
		moderationReason: null,
		reviewedBy: null,
		reviewedAt: null,
		deletedBy: null,
		deletedAt: null,
		author: null,
		createdAt: '2026-08-12T00:00:00Z',
	};
}

describe('useAdminMessageList', () => {
	beforeEach(() => vi.clearAllMocks());

	it('builds all confirmed message filters', async () => {
		vi.mocked(listMessages).mockResolvedValue(page());
		const { filterForm, getMessageList } = useAdminMessageList();
		filterForm.messageId = 90001;
		filterForm.userId = 10001;
		filterForm.guestNickname = '访客';
		filterForm.guestEmail = 'guest@example.com';
		filterForm.content = '内容';
		filterForm.status = MESSAGE_STATUS.PENDING;
		filterForm.type = MESSAGE_TYPE.TOP_LEVEL;
		filterForm.createdAtRange = ['2026-08-01T00:00:00Z', '2026-08-31T23:59:59Z'];

		await getMessageList(2);

		expect(listMessages).toHaveBeenCalledWith({
			pageNum: 2,
			pageSize: 10,
			messageId: 90001,
			userId: 10001,
			guestNickname: '访客',
			guestEmail: 'guest@example.com',
			content: '内容',
			status: MESSAGE_STATUS.PENDING,
			type: MESSAGE_TYPE.TOP_LEVEL,
			createdAtFrom: '2026-08-01T00:00:00Z',
			createdAtTo: '2026-08-31T23:59:59Z',
		});
	});

	it('uses the selected IDs for batch approval and refreshes the page', async () => {
		vi.mocked(approveMessages).mockResolvedValue(null);
		vi.mocked(listMessages).mockResolvedValue(page());
		const { selectedMessageIds, approveSelected } = useAdminMessageList();
		selectedMessageIds.value = [90001, 90002];

		await approveSelected();

		expect(approveMessages).toHaveBeenCalledWith({ messageIds: [90001, 90002] });
		expect(listMessages).toHaveBeenCalledTimes(1);
	});

	it('prevents duplicate batch approval requests while submitting', async () => {
		let resolveApproval!: () => void;
		vi.mocked(approveMessages).mockReturnValue(
			new Promise<null>((resolve) => {
				resolveApproval = () => resolve(null);
			}),
		);
		vi.mocked(listMessages).mockResolvedValue(page());
		const { selectedMessageIds, submitting, approveSelected } = useAdminMessageList();
		selectedMessageIds.value = [90001];

		const firstRequest = approveSelected();
		const duplicateResult = await approveSelected();

		expect(submitting.value).toBe(true);
		expect(duplicateResult).toBe(false);
		expect(approveMessages).toHaveBeenCalledTimes(1);

		resolveApproval();
		await firstRequest;
		expect(submitting.value).toBe(false);
	});

	it('keeps the reply flow open when the request fails', async () => {
		vi.mocked(replyMessage).mockRejectedValue(new Error('network failed'));
		const { submitting, reply } = useAdminMessageList();

		const success = await reply(message(MESSAGE_STATUS.APPROVED), '管理员回复');

		expect(success).toBe(false);
		expect(submitting.value).toBe(false);
	});

	it('exposes actions that match the message status transitions', () => {
		const { canApprove, canReply, canReject, canHide, canDelete } = useAdminMessageList();

		expect(canApprove(message(MESSAGE_STATUS.PENDING))).toBe(true);
		expect(canApprove(message(MESSAGE_STATUS.REJECTED))).toBe(true);
		expect(canApprove(message(MESSAGE_STATUS.HIDDEN))).toBe(true);
		expect(canApprove(message(MESSAGE_STATUS.HIDDEN, 90000))).toBe(true);
		expect(canReply(message(MESSAGE_STATUS.APPROVED))).toBe(true);
		expect(canReply(message(MESSAGE_STATUS.APPROVED, 90000))).toBe(false);
		expect(canReject(message(MESSAGE_STATUS.PENDING))).toBe(true);
		expect(canHide(message(MESSAGE_STATUS.APPROVED))).toBe(true);
		expect(canDelete(message(MESSAGE_STATUS.HIDDEN))).toBe(true);
		expect(canDelete(message(MESSAGE_STATUS.DELETED))).toBe(false);
	});
});
