import { computed, reactive, ref } from 'vue';
import { createGlobalState } from '@vueuse/core';
import {
	getUnreadCount,
	listNotifications,
	markAllNotificationsRead,
	markNotificationRead,
} from '../api/notificationApi';
import {
	NOTIFICATION_CATEGORY,
	type NotificationCategory,
	type NotificationItem,
} from '../types/notification';

export const useNotificationUnread = createGlobalState(() => {
	const unreadCount = ref({ total: 0, reply: 0, adminMessage: 0 });
	let requestVersion = 0;

	const hasUnread = computed(() => unreadCount.value.total > 0);

	async function loadUnreadCount() {
		const version = ++requestVersion;
		try {
			const count = await getUnreadCount();
			if (version === requestVersion) unreadCount.value = count;
		} catch {
			// 错误提示由统一响应拦截器处理。
		}
	}

	function clearUnreadCount() {
		requestVersion++;
		unreadCount.value = { total: 0, reply: 0, adminMessage: 0 };
	}

	return { unreadCount, hasUnread, loadUnreadCount, clearUnreadCount };
});

export function useNotifications() {
	const notifications = ref<NotificationItem[]>([]);
	const loading = ref(false);
	const page = reactive({ pageNum: 1, pageSize: 20, hasNext: false });
	const category = ref<NotificationCategory>(NOTIFICATION_CATEGORY.ALL);
	const { unreadCount, hasUnread, loadUnreadCount } = useNotificationUnread();
	let requestId = 0;
	const hasUnreadInCategory = computed(() => {
		if (category.value === NOTIFICATION_CATEGORY.REPLY) return unreadCount.value.reply > 0;
		if (category.value === NOTIFICATION_CATEGORY.ADMIN_MESSAGE)
			return unreadCount.value.adminMessage > 0;
		return hasUnread.value;
	});

	async function loadNotifications(pageNum = 1) {
		if (pageNum > 1 && loading.value) return;
		const currentRequestId = ++requestId;
		loading.value = true;
		try {
			const data = await listNotifications({
				pageNum,
				pageSize: page.pageSize,
				category: category.value,
			});
			if (currentRequestId !== requestId) return;
			notifications.value =
				pageNum === 1 ? data.records : [...notifications.value, ...data.records];
			page.pageNum = data.pageNum;
			page.hasNext = data.hasNext;
		} catch {
			// 错误提示由统一响应拦截器处理。
		} finally {
			if (currentRequestId === requestId) loading.value = false;
		}
	}

	async function changeCategory(nextCategory: NotificationCategory) {
		category.value = nextCategory;
		await loadNotifications(1);
	}

	async function markRead(notification: NotificationItem) {
		if (notification.read) return;
		await markNotificationRead(notification.id);
		notification.read = true;
		await loadUnreadCount();
	}

	async function markAllRead() {
		const selectedCategory = category.value;
		await markAllNotificationsRead(selectedCategory);
		if (category.value === selectedCategory) {
			notifications.value.forEach((notification) => {
				notification.read = true;
			});
		}
		await loadUnreadCount();
	}

	return {
		notifications,
		page,
		category,
		unreadCount,
		hasUnread,
		hasUnreadInCategory,
		loading,
		loadUnreadCount,
		loadNotifications,
		changeCategory,
		markRead,
		markAllRead,
	};
}
