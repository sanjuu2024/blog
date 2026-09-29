import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getUnreadCount, listNotifications, markNotificationRead } from '../api/notificationApi';
import { likeComment } from '@/modules/comment/api/commentApi';
import { useNotificationUnread, useNotifications } from './useNotifications';

vi.mock('../api/notificationApi', () => ({
	getUnreadCount: vi.fn(),
	listNotifications: vi.fn(),
	markAllNotificationsRead: vi.fn(),
	markNotificationRead: vi.fn(),
}));
vi.mock('@/modules/comment/api/commentApi', () => ({
	likeComment: vi.fn(),
	unlikeComment: vi.fn(),
}));

describe('useNotifications', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		useNotificationUnread().clearUnreadCount();
	});

	it('shares unread state with the avatar menu after marking a notification read', async () => {
		vi.mocked(getUnreadCount)
			.mockResolvedValueOnce({ total: 1, reply: 1, adminMessage: 0 })
			.mockResolvedValueOnce({ total: 0, reply: 0, adminMessage: 0 });
		vi.mocked(markNotificationRead).mockResolvedValue(null);
		const menu = useNotificationUnread();
		const center = useNotifications();

		await menu.loadUnreadCount();
		expect(menu.hasUnread.value).toBe(true);
		await center.markRead({
			id: 1,
			type: 'COMMENT_REPLY',
			title: '回复',
			content: '内容',
			sourceId: 30001,
			authorName: '回复者',
			originalContent: '原评论',
			articleId: 40001,
			parentId: 30000,
			likeCount: 0,
			liked: false,
			canInteract: true,
			read: false,
			createdAt: '2026-09-28T00:00:00Z',
		});

		expect(menu.hasUnread.value).toBe(false);
	});

	it('does not restore the previous users unread count after logout', async () => {
		let resolveCount!: (count: { total: number; reply: number; adminMessage: number }) => void;
		vi.mocked(getUnreadCount).mockReturnValue(
			new Promise((resolve) => {
				resolveCount = resolve;
			}),
		);
		const menu = useNotificationUnread();
		const loading = menu.loadUnreadCount();

		menu.clearUnreadCount();
		resolveCount({ total: 1, reply: 1, adminMessage: 0 });
		await loading;

		expect(menu.hasUnread.value).toBe(false);
	});

	it('ignores an older category response after switching tabs', async () => {
		let resolveFirst!: (data: Awaited<ReturnType<typeof listNotifications>>) => void;
		vi.mocked(listNotifications)
			.mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
			.mockResolvedValueOnce({
				total: 1,
				totalPages: 1,
				pageNum: 1,
				pageSize: 20,
				hasNext: false,
				records: [
					{
						id: 2,
						type: 'ADMIN_MESSAGE',
						title: '管理员消息',
						content: '内容',
						sourceId: null,
						authorName: null,
						originalContent: null,
						articleId: null,
						parentId: null,
						likeCount: 0,
						liked: false,
						canInteract: false,
						read: false,
						createdAt: '',
					},
				],
			});
		const center = useNotifications();
		const firstRequest = center.loadNotifications();

		await center.changeCategory('ADMIN_MESSAGE');
		resolveFirst({
			total: 1,
			totalPages: 1,
			pageNum: 1,
			pageSize: 20,
			hasNext: false,
			records: [
				{
					id: 1,
					type: 'COMMENT_REPLY',
					title: '回复',
					content: '旧结果',
					sourceId: 30001,
					authorName: '回复者',
					originalContent: '原评论',
					articleId: 40001,
					parentId: 30000,
					likeCount: 0,
					liked: false,
					canInteract: true,
					read: false,
					createdAt: '',
				},
			],
		});
		await firstRequest;

		expect(center.notifications.value.map((item) => item.id)).toEqual([2]);
	});

	it('updates reply like count from the existing comment API', async () => {
		vi.mocked(likeComment).mockResolvedValue({ liked: true, likeCount: 2 });
		const center = useNotifications();
		const comment = {
			id: 1,
			type: 'COMMENT_REPLY' as const,
			title: '回复',
			content: '内容',
			sourceId: 30001,
			authorName: '用户',
			originalContent: '原文',
			articleId: 40001,
			parentId: 30000,
			likeCount: 1,
			liked: false,
			canInteract: true,
			read: false,
			createdAt: '',
		};
		await center.toggleLike(comment);

		expect(likeComment).toHaveBeenCalledWith(30001);
		expect(comment.likeCount).toBe(2);
	});
});
