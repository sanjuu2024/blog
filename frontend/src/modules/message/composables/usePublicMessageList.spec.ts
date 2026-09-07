import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMessage, deleteMessage, listMessages } from '../api/messageApi';
import { ElMessageBox } from 'element-plus';
import {
	MESSAGE_STATUS,
	type MessageMutationData,
	type PublicMessagePageData,
} from '../types/message';
import { usePublicMessageList } from './usePublicMessageList';

vi.mock('../api/messageApi', () => ({
	createMessage: vi.fn(),
	deleteMessage: vi.fn(),
	listMessages: vi.fn(),
}));

vi.mock('element-plus', () => ({
	ElMessage: { success: vi.fn() },
	ElMessageBox: { confirm: vi.fn() },
}));

function page(overrides: Partial<PublicMessagePageData> = {}): PublicMessagePageData {
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

function mutation(): MessageMutationData {
	return {
		id: 90001,
		parentId: null,
		nickname: '访客',
		content: '留言内容',
		status: MESSAGE_STATUS.PENDING,
		moderationReason: null,
		author: null,
		isMine: false,
		replies: [],
		createdAt: '2026-08-12T00:00:00Z',
	};
}

describe('usePublicMessageList', () => {
	beforeEach(() => vi.clearAllMocks());

	it('does not insert a guest pending message into the public list', async () => {
		vi.mocked(createMessage).mockResolvedValue(mutation());
		const { messageList, submitMessage } = usePublicMessageList();

		await submitMessage({ nickname: '访客', content: '留言内容' });

		expect(messageList.value).toEqual([]);
	});

	it('inserts a logged-in pending message at the top without reloading', async () => {
		vi.mocked(createMessage).mockResolvedValue({ ...mutation(), isMine: true });
		const { messageList, submitMessage } = usePublicMessageList();

		await submitMessage({ content: '留言内容' });

		expect(messageList.value.map((item) => item.id)).toEqual([90001]);
		expect(listMessages).not.toHaveBeenCalled();
	});

	it('appends the next page when loading more', async () => {
		vi.mocked(listMessages)
			.mockResolvedValueOnce(
				page({ hasNext: true, records: [{ ...mutation(), isMine: false }] }),
			)
			.mockResolvedValueOnce(
				page({
					pageNum: 2,
					records: [{ ...mutation(), id: 90002, isMine: false }],
				}),
			);
		const { messageList, getMessageList, loadMoreMessages } = usePublicMessageList();

		await getMessageList();
		await loadMoreMessages();

		expect(messageList.value.map((item) => item.id)).toEqual([90001, 90002]);
	});

	it('does not duplicate a locally inserted message when loading more', async () => {
		const localMessage = { ...mutation(), isMine: true };
		vi.mocked(createMessage).mockResolvedValue(localMessage);
		vi.mocked(listMessages).mockResolvedValue(
			page({
				pageNum: 2,
				records: [localMessage, { ...mutation(), id: 90002, isMine: false }],
			}),
		);
		const { messageList, pageParams, submitMessage, loadMoreMessages } = usePublicMessageList();
		pageParams.hasNext = true;

		await submitMessage({ content: '留言内容' });
		await loadMoreMessages();

		expect(messageList.value.map((item) => item.id)).toEqual([90001, 90002]);
	});

	it('reloads the first page after deleting a message', async () => {
		const deleted = { ...mutation(), isMine: true };
		const remaining = { ...mutation(), id: 90002, isMine: false };
		vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never);
		vi.mocked(deleteMessage).mockResolvedValue(null);
		vi.mocked(listMessages).mockResolvedValue(page({ records: [remaining] }));
		const { messageList, removeMessage } = usePublicMessageList();
		messageList.value = [deleted, remaining];

		await removeMessage(deleted);

		expect(deleteMessage).toHaveBeenCalledWith(deleted.id);
		expect(listMessages).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 });
		expect(messageList.value).toEqual([remaining]);
	});

	it('ignores a cancelled delete confirmation', async () => {
		vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel');
		const { removeMessage } = usePublicMessageList();

		await expect(removeMessage({ ...mutation(), isMine: true })).resolves.toBeUndefined();

		expect(deleteMessage).not.toHaveBeenCalled();
	});
});
