import { ElMessage } from 'element-plus';
import {
	deleteAdminFriendLink,
	listAdminFriendLinks,
	updateAdminFriendLink,
} from '../api/adminFriendLinkApi';
import type {
	AdminFriendLinkFilterForm,
	AdminFriendLinkItem,
	AdminFriendLinkListQuery,
	AdminFriendLinkPageData,
	FriendLinkUpsertRequest,
} from '../types/adminFriendLink';

export function useAdminFriendLinkList() {
	let requestId = 0;
	const friendLinks = ref<AdminFriendLinkItem[]>([]);
	const loading = ref(false);
	const pageMeta = reactive({ total: 0, totalPages: 0, hasNext: false });
	const pageParams = reactive({ pageNum: 1, pageSize: 10 });
	const initFilterForm: AdminFriendLinkFilterForm = { keyword: '', section: '', status: '' };
	const filterForm = reactive<AdminFriendLinkFilterForm>({ ...initFilterForm });

	function buildQuery(): AdminFriendLinkListQuery {
		return {
			pageNum: pageParams.pageNum,
			pageSize: pageParams.pageSize,
			keyword: filterForm.keyword.trim() || undefined,
			section: filterForm.section || undefined,
			status: filterForm.status || undefined,
		};
	}

	async function getFriendLinkList(page: number = pageParams.pageNum) {
		const currentRequestId = ++requestId;
		pageParams.pageNum = page;
		loading.value = true;
		try {
			const data: AdminFriendLinkPageData = await listAdminFriendLinks(buildQuery());
			if (currentRequestId !== requestId) return;
			friendLinks.value = data.records;
			pageParams.pageNum = data.pageNum;
			pageParams.pageSize = data.pageSize;
			pageMeta.total = data.total;
			pageMeta.totalPages = data.totalPages;
			pageMeta.hasNext = data.hasNext;
		} catch {
			// 请求错误由统一响应拦截器提示。
		} finally {
			if (currentRequestId === requestId) loading.value = false;
		}
	}

	function resetFilterForm() {
		Object.assign(filterForm, initFilterForm);
		getFriendLinkList(1);
	}

	async function handleDeleteFriendLink(friendLink: AdminFriendLinkItem) {
		try {
			await deleteAdminFriendLink(friendLink.id);
			ElMessage.success('友链删除成功');
			await getFriendLinkList(1);
		} catch {
			// 请求错误由统一响应拦截器提示。
		}
	}

	async function toggleFriendLinkStatus(friendLink: AdminFriendLinkItem) {
		const nextStatus = friendLink.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
		const request: FriendLinkUpsertRequest = {
			name: friendLink.name,
			url: friendLink.url,
			logoUrl: friendLink.logoUrl,
			description: friendLink.description,
			section: friendLink.section,
			sortNo: friendLink.sortNo,
			status: nextStatus,
		};
		try {
			const updated = await updateAdminFriendLink(friendLink.id, request);
			const index = friendLinks.value.findIndex((item) => item.id === friendLink.id);
			if (index >= 0) friendLinks.value[index] = updated;
			ElMessage.success('友链状态已更新');
		} catch {
			// 请求错误由统一响应拦截器提示。
		}
	}

	return {
		friendLinks,
		loading,
		pageMeta,
		pageParams,
		filterForm,
		getFriendLinkList,
		resetFilterForm,
		handleDeleteFriendLink,
		toggleFriendLinkStatus,
	};
}
